package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.api.request.v1.AccessMode;
import no.mnemonic.services.grafeo.api.request.v1.CreateFactRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.contexts.TriggerContext;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.GrafeoServiceEvent;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactCreateHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.ObjectRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactCreateDelegateTest {

  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private ObjectRequestResolver objectRequestResolver;
  @Mock
  private FactCreateHandler factCreateHandler;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private TriggerContext triggerContext;
  @Mock
  private Clock clock;

  private FactCreateDelegate delegate;

  private final OriginEntity origin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setName("origin")
          .setTrust(0.1f);
  private final Organization organization = Organization.builder()
          .setId(UUID.randomUUID())
          .setName("organization")
          .build();
  private final Subject subject = Subject.builder()
          .setId(UUID.randomUUID())
          .setName("subject")
          .build();
  private final ObjectTypeEntity ipObjectType = new ObjectTypeEntity()
          .setId(UUID.randomUUID())
          .setName("ip");
  private final ObjectTypeEntity domainObjectType = new ObjectTypeEntity()
          .setId(UUID.randomUUID())
          .setName("domain");
  private final FactTypeEntity resolveFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("resolve")
          .setValidator("validator")
          .setValidatorParameter("validatorParameter")
          .setDefaultConfidence(0.2f)
          .setRelevantObjectBindings(set(
                  // Allow bindings with both cardinality 1 and 2.
                  new FactTypeEntity.FactObjectBindingDefinition().setSourceObjectTypeID(ipObjectType.getId()).setDestinationObjectTypeID(domainObjectType.getId()),
                  new FactTypeEntity.FactObjectBindingDefinition().setSourceObjectTypeID(ipObjectType.getId()),
                  new FactTypeEntity.FactObjectBindingDefinition().setDestinationObjectTypeID(domainObjectType.getId())
          ));
  private final ObjectRecord ip = new ObjectRecord()
          .setId(UUID.randomUUID())
          .setTypeID(ipObjectType.getId())
          .setValue("1.2.3.4");
  private final ObjectRecord domain = new ObjectRecord()
          .setId(UUID.randomUUID())
          .setTypeID(domainObjectType.getId())
          .setValue("test.example.org");
  private final ObjectRecord threatActor = new ObjectRecord()
          .setId(UUID.randomUUID())
          .setTypeID(UUID.randomUUID())
          .setValue("APT1");

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactCreateDelegate(
            securityContext,
            triggerContext,
            factTypeRequestResolver,
            objectRequestResolver,
            factCreateHandler,
            objectManager
    ).withClock(clock);

    when(clock.millis()).thenReturn(1000L, 2000L, 3000L);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactWithoutAddPermission() throws Exception {
    mockFetchingOrganization();
    mockFetchingFactType();
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.addThreatIntelFact, organization.getId());
    delegate.handle(createRequest());
  }

  @Test
  public void testFactValueIsValidated() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).assertValidFactValue(any(), eq(request.getValue()));
  }

  @Test
  public void testValidateBindingsFailsWithoutObjects() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject("unknown")
            .setDestinationObject("unknown");
    mockCreateNewFact();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.source.object", "invalid.destination.object"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));

    verify(objectRequestResolver, times(2)).resolveObject(eq("unknown"), anyString());
  }

  @Test
  public void testValidateBindingsFailsWithSameObject() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject(ip.getId().toString())
            .setDestinationObject(ip.getId().toString());
    mockCreateNewFact();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.source.object", "invalid.destination.object"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidateBindingsFailsWithoutBindingsOnFactType() throws Exception {
    CreateFactRequest request = createRequest();
    resolveFactType.setRelevantObjectBindings(null);
    mockCreateNewFact();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.fact.object.binding"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidateBindingsFailsOnType() throws Exception {
    CreateFactRequest request = createRequest()
            .setDestinationObject(threatActor.getId().toString());
    mockCreateNewFact();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.fact.object.binding"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidateBindingsFailsOnDirection() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject(domain.getId().toString())
            .setDestinationObject(ip.getId().toString());
    mockCreateNewFact();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.fact.object.binding"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidateBindingsFailsOnBidirectionalBinding() throws Exception {
    CreateFactRequest request = createRequest()
            .setBidirectionalBinding(true);
    mockCreateNewFact();

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("invalid.fact.object.binding"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testCreateFactWithOnlySourceObject() throws Exception {
    CreateFactRequest request = createRequest()
            .setDestinationObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), notNull(), notNull());
  }

  @Test
  public void testCreateFactWithOnlySourceObjectAndTimeGlobal() throws Exception {
    ipObjectType.addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
    CreateFactRequest request = createRequest()
            .setDestinationObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(record -> record.isSet(FactRecord.Flag.TimeGlobalIndex)), notNull(), notNull());
  }

  @Test
  public void testCreateFactWithOnlyDestinationObject() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), notNull(), notNull());
  }

  @Test
  public void testCreateFactWithOnlyDestinationObjectAndTimeGlobal() throws Exception {
    domainObjectType.addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
    CreateFactRequest request = createRequest()
            .setSourceObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(record -> record.isSet(FactRecord.Flag.TimeGlobalIndex)), notNull(), notNull());
  }

  @Test
  public void testCreateFactWithBothSourceAndDestinationObjects() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), notNull(), notNull());
  }

  @Test
  public void testCreateFactWithBothSourceAndDestinationObjectsAndTimeGlobal() throws Exception {
    ipObjectType.addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
    domainObjectType.addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(record -> record.isSet(FactRecord.Flag.TimeGlobalIndex)), notNull(), notNull());
  }

  @Test
  public void testCreateFactWithBothSourceAndDestinationObjectsAndNoTimeGlobal() throws Exception {
    ipObjectType.addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(record -> !record.isSet(FactRecord.Flag.TimeGlobalIndex)), notNull(), notNull());
  }

  @Test
  public void testCreateFactWithBidirectionalBinding() throws Exception {
    CreateFactRequest request = createRequest()
            .setBidirectionalBinding(true);
    // Need to change type in order to pass validation.
    resolveFactType.setRelevantObjectBindings(set(new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(ipObjectType.getId())
            .setDestinationObjectTypeID(domainObjectType.getId())
            .setBidirectionalBinding(true)
    ));
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), notNull(), notNull());
  }

  @Test
  public void testCreateFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    CreateFactRequest request = createRequest()
            .setOrganization(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveOrganization(isNull(), eq(origin)))
            .thenReturn(Organization.builder().setId(organizationID).build());

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(organizationID, e.getOrganizationID())), notNull(), notNull());
  }

  @Test
  public void testCreateFactSetMissingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    CreateFactRequest request = createRequest()
            .setOrigin(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveOrigin(isNull())).thenReturn(new OriginEntity().setId(originID));
    when(factCreateHandler.resolveOrganization(notNull(), notNull())).thenReturn(organization);

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(originID, e.getOriginID())), notNull(), notNull());
  }

  @Test
  public void testCreateFactSetMissingConfidence() throws Exception {
    CreateFactRequest request = createRequest()
            .setConfidence(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(argThat(e -> Objects.equals(resolveFactType.getDefaultConfidence(), e.getConfidence())), notNull(), notNull());
  }

  @Test
  public void testCreateFactSavesCommentAndAcl() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), eq(request.getComment()), eq(list(subject.getId())));
  }

  @Test
  public void testCreateFactRegisterTriggerEvent() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    Fact addedFact = delegate.handle(request);

    verify(triggerContext).registerTriggerEvent(argThat(event -> {
      assertNotNull(event);
      assertEquals(GrafeoServiceEvent.EventName.FactAdded.name(), event.getEvent());
      assertEquals(addedFact.getOrganization().getId(), event.getOrganization());
      assertEquals(addedFact.getAccessMode().name(), event.getAccessMode().name());
      assertSame(addedFact, event.getContextParameters().get(GrafeoServiceEvent.ContextParameter.AddedFact.name()));
      return true;
    }));
  }

  private void mockCreateNewFact() throws Exception {
    mockFetchingOrganization();
    mockFetchingSubject();
    mockFetchingFactType();
    mockFetchingObjects();

    // Mock fetching ObjectType (required to determine time global index).
    when(objectManager.getObjectType(ipObjectType.getId())).thenReturn(ipObjectType);
    when(objectManager.getObjectType(domainObjectType.getId())).thenReturn(domainObjectType);
    // Mock fetching of current user.
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    // Mocking needed for registering TriggerEvent.
    when(factCreateHandler.saveFact(any(), any(), any())).then(i -> {
      FactRecord record = i.getArgument(0);
      return Fact.builder()
              .setId(record.getId())
              .setAccessMode(no.mnemonic.services.grafeo.api.model.v1.AccessMode.valueOf(record.getAccessMode().name()))
              .setOrganization(Organization.builder().setId(record.getOrganizationID()).build().toInfo())
              .build();
    });
  }

  private void mockFetchingOrganization() throws Exception {
    when(factCreateHandler.resolveOrigin(origin.getName())).thenReturn(origin);
    when(factCreateHandler.resolveOrganization(organization.getName(), origin)).thenReturn(organization);
  }

  private void mockFetchingSubject() throws Exception {
    when(factCreateHandler.resolveSubjects(notNull())).thenReturn(list(subject));
  }

  private void mockFetchingFactType() throws Exception {
    when(factTypeRequestResolver.resolveFactType(resolveFactType.getName())).thenReturn(resolveFactType);
  }

  private void mockFetchingObjects() throws Exception {
    when(objectRequestResolver.resolveObject(eq(ip.getId().toString()), anyString())).thenReturn(ip);
    when(objectRequestResolver.resolveObject(eq(domain.getId().toString()), anyString())).thenReturn(domain);
    when(objectRequestResolver.resolveObject(eq(threatActor.getId().toString()), anyString())).thenReturn(threatActor);
  }

  private CreateFactRequest createRequest() {
    return new CreateFactRequest()
            .setType(resolveFactType.getName())
            .setValue("factValue")
            .setOrganization(organization.getName())
            .setOrigin(origin.getName())
            .setConfidence(0.3f)
            .setComment("Hello World!")
            .setAccessMode(AccessMode.RoleBased)
            .addAcl(subject.getName())
            .setSourceObject(ip.getId().toString())
            .setDestinationObject(domain.getId().toString());
  }

  private FactRecord matchFactRecord(CreateFactRequest request) {
    return argThat(record -> {
      assertNotNull(record.getId());
      assertEquals(resolveFactType.getId(), record.getTypeID());
      assertEquals(request.getValue(), record.getValue());
      assertEquals(organization.getId(), record.getOrganizationID());
      assertNotNull(record.getAddedByID());
      assertNotNull(record.getLastSeenByID());
      assertEquals(origin.getId(), record.getOriginID());
      assertEquals(origin.getTrust(), record.getTrust(), 0.0);
      assertEquals(request.getConfidence(), record.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), record.getAccessMode().name());
      assertTrue(record.getTimestamp() > 0);
      assertTrue(record.getLastSeenTimestamp() > 0);
      assertEquals(record.getTimestamp(), record.getLastSeenTimestamp());
      assertEquals(request.isBidirectionalBinding(), record.isBidirectionalBinding());

      if (request.getSourceObject() != null) {
        assertEquals(UUID.fromString(request.getSourceObject()), record.getSourceObject().getId());
      }

      if (request.getDestinationObject() != null) {
        assertEquals(UUID.fromString(request.getDestinationObject()), record.getDestinationObject().getId());
      }

      return true;
    });
  }
}
