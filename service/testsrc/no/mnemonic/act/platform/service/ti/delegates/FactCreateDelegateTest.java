package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.dao.api.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.helpers.FactCreateHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.act.platform.service.ti.resolvers.ObjectResolver;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FactCreateDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private ObjectResolver objectResolver;
  @Mock
  private FactCreateHelper factCreateHelper;
  @Mock
  private FactStorageHelper factStorageHelper;

  private FactCreateDelegate delegate;

  private final OriginEntity origin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setName("origin")
          .setTrust(0.1f);
  private final Organization organization = Organization.builder()
          .setId(UUID.randomUUID())
          .setName("organization")
          .build();
  private final ObjectTypeEntity ipObjectType = new ObjectTypeEntity()
          .setId(UUID.randomUUID())
          .setName("ip");
  private final ObjectTypeEntity domainObjectType = new ObjectTypeEntity()
          .setId(UUID.randomUUID())
          .setName("domain");
  private final FactTypeEntity retractionFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("Retraction");
  private final FactTypeEntity resolveFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("resolve")
          .setValidator("validator")
          .setValidatorParameter("validatorParameter")
          .setDefaultConfidence(0.2f)
          .setRelevantObjectBindings(SetUtils.set(
                  // Allow bindings with both cardinality 1 and 2.
                  new FactTypeEntity.FactObjectBindingDefinition().setSourceObjectTypeID(ipObjectType.getId()).setDestinationObjectTypeID(domainObjectType.getId()),
                  new FactTypeEntity.FactObjectBindingDefinition().setSourceObjectTypeID(ipObjectType.getId()),
                  new FactTypeEntity.FactObjectBindingDefinition().setDestinationObjectTypeID(domainObjectType.getId())
          ));
  private final ObjectEntity ip = new ObjectEntity()
          .setId(UUID.randomUUID())
          .setTypeID(ipObjectType.getId())
          .setValue("1.2.3.4");
  private final ObjectEntity domain = new ObjectEntity()
          .setId(UUID.randomUUID())
          .setTypeID(domainObjectType.getId())
          .setValue("test.example.org");
  private final ObjectEntity threatActor = new ObjectEntity()
          .setId(UUID.randomUUID())
          .setTypeID(UUID.randomUUID())
          .setValue("APT1");

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactCreateDelegate(
            getSecurityContext(),
            getTriggerContext(),
            getFactManager(),
            getFactSearchManager(),
            getObjectManager(),
            factTypeResolver,
            objectResolver,
            factCreateHelper,
            factStorageHelper,
            getFactConverter()
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactWithRetractionFactTypeThrowsException() throws Exception {
    mockFetchingFactType();
    delegate.handle(new CreateFactRequest().setType(retractionFactType.getName()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactWithoutAddPermission() throws Exception {
    mockFetchingOrganization();
    mockFetchingFactType();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addFactObjects, organization.getId());
    delegate.handle(createRequest());
  }

  @Test
  public void testValidateFactValueThrowsException() throws Exception {
    CreateFactRequest request = createRequest();
    mockFetchingOrganization();
    mockFetchingFactType();
    Validator validatorMock = mockValidator(false);

    expectInvalidArgumentException(() -> delegate.handle(request), "fact.not.valid");

    verify(validatorMock).validate(request.getValue());
  }

  @Test
  public void testValidateBindingsFailsWithoutObjects() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject("unknown")
            .setDestinationObject("unknown");
    mockCreateNewFact();

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.source.object", "invalid.destination.object");

    verify(objectResolver, times(2)).resolveObject("unknown");
  }

  @Test
  public void testValidateBindingsFailsWithoutBindingsOnFactType() throws Exception {
    CreateFactRequest request = createRequest();
    resolveFactType.setRelevantObjectBindings(null);
    mockCreateNewFact();

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.fact.object.binding");
  }

  @Test
  public void testValidateBindingsFailsOnType() throws Exception {
    CreateFactRequest request = createRequest()
            .setDestinationObject(threatActor.getId().toString());
    mockCreateNewFact();

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.fact.object.binding");
  }

  @Test
  public void testValidateBindingsFailsOnDirection() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject(domain.getId().toString())
            .setDestinationObject(ip.getId().toString());
    mockCreateNewFact();

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.fact.object.binding");
  }

  @Test
  public void testValidateBindingsFailsOnBidirectionalBinding() throws Exception {
    CreateFactRequest request = createRequest()
            .setBidirectionalBinding(true);
    mockCreateNewFact();

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.fact.object.binding");
  }

  @Test
  public void testCreateFactWithOnlySourceObject() throws Exception {
    CreateFactRequest request = createRequest()
            .setDestinationObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(matchFactEntity(request));
    verify(getObjectManager()).saveObjectFactBinding(matchObjectFactBindingEntity());
    verify(getFactSearchManager()).indexFact(matchFactDocument(request));
  }

  @Test
  public void testCreateFactWithOnlyDestinationObject() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(matchFactEntity(request));
    verify(getObjectManager()).saveObjectFactBinding(matchObjectFactBindingEntity());
    verify(getFactSearchManager()).indexFact(matchFactDocument(request));
  }

  @Test
  public void testCreateFactWithBothSourceAndDestinationObjects() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(matchFactEntity(request));
    verify(getObjectManager(), times(2)).saveObjectFactBinding(matchObjectFactBindingEntity());
    verify(factStorageHelper).saveInitialAclForNewFact(matchFactEntity(request), eq(request.getAcl()));
    verify(factStorageHelper).saveCommentForFact(matchFactEntity(request), eq(request.getComment()));
    verify(getFactSearchManager()).indexFact(matchFactDocument(request));
    verify(getFactConverter()).apply(matchFactEntity(request));
  }

  @Test
  public void testCreateFactWithBidirectionalBinding() throws Exception {
    CreateFactRequest request = createRequest()
            .setBidirectionalBinding(true);
    // Need to change type in order to pass validation.
    resolveFactType.setRelevantObjectBindings(SetUtils.set(new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(ipObjectType.getId())
            .setDestinationObjectTypeID(domainObjectType.getId())
            .setBidirectionalBinding(true)
    ));
    mockCreateNewFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> e.getBindings().stream().allMatch(b -> b.getDirection().equals(Direction.BiDirectional))));
    verify(getObjectManager(), times(2)).saveObjectFactBinding(argThat(e -> e.getDirection().equals(Direction.BiDirectional)));
    verify(getFactSearchManager()).indexFact(argThat(e -> e.getObjects().stream().allMatch(o -> o.getDirection().equals(ObjectDocument.Direction.BiDirectional))));
  }

  @Test
  public void testCreateFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    CreateFactRequest request = createRequest()
            .setOrganization(null);
    mockCreateNewFact();

    when(factCreateHelper.resolveOrganization(isNull(), eq(origin)))
            .thenReturn(Organization.builder().setId(organizationID).build());

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> organizationID.equals(e.getOrganizationID())));
    verify(getFactConverter()).apply(argThat(e -> organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testCreateFactSetMissingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    CreateFactRequest request = createRequest()
            .setOrigin(null);
    mockCreateNewFact();

    when(factCreateHelper.resolveOrigin(isNull())).thenReturn(new OriginEntity().setId(originID));
    when(factCreateHelper.resolveOrganization(notNull(), notNull())).thenReturn(organization);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> originID.equals(e.getOriginID())));
    verify(getFactConverter()).apply(argThat(e -> originID.equals(e.getOriginID())));
  }

  @Test
  public void testCreateFactSetMissingConfidence() throws Exception {
    CreateFactRequest request = createRequest()
            .setConfidence(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> Objects.equals(resolveFactType.getDefaultConfidence(), e.getConfidence())));
    verify(getFactConverter()).apply(argThat(e -> Objects.equals(resolveFactType.getDefaultConfidence(), e.getConfidence())));
  }

  @Test
  public void testCreateFactRegistersTriggerEvent() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    Fact addedFact = delegate.handle(request);

    verify(getTriggerContext()).registerTriggerEvent(argThat(event -> {
      assertNotNull(event);
      assertEquals(TiServiceEvent.EventName.FactAdded.name(), event.getEvent());
      assertEquals(request.getOrganization(), event.getOrganization());
      assertEquals(request.getAccessMode().name(), event.getAccessMode().name());
      assertSame(addedFact, event.getContextParameters().get(TiServiceEvent.ContextParameter.AddedFact.name()));
      return true;
    }));
  }

  @Test
  public void testRefreshExistingFact() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    FactEntity existingFact = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(resolveFactType.getId())
            .setValue(request.getValue())
            .setOriginID(request.getOrigin())
            .setOrganizationID(request.getOrganization())
            .setAccessMode(no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.valueOf(request.getAccessMode().name()))
            .setConfidence(request.getConfidence())
            .setLastSeenTimestamp(123)
            .setBindings(ListUtils.list(
                    new FactEntity.FactObjectBinding().setObjectID(UUID.fromString(request.getSourceObject())).setDirection(Direction.FactIsDestination),
                    new FactEntity.FactObjectBinding().setObjectID(UUID.fromString(request.getDestinationObject())).setDirection(Direction.FactIsSource)
            ));

    // Mock fetching of existing Fact.
    when(getFactSearchManager().retrieveExistingFacts(matchFactExistenceSearchCriteria(request)))
            .thenReturn(SearchResult.<FactDocument>builder()
                    .setCount(1)
                    .addValue(new FactDocument().setId(existingFact.getId()))
                    .build());
    when(getFactManager().getFacts(ListUtils.list(existingFact.getId()))).thenReturn(ListUtils.list(existingFact).iterator());
    when(getSecurityContext().hasReadPermission(existingFact)).thenReturn(true);

    // Mock stuff needed for refreshing Fact.
    when(getFactManager().refreshFact(existingFact.getId())).thenReturn(existingFact);
    when(getFactSearchManager().getFact(existingFact.getId())).thenReturn(new FactDocument());
    when(factStorageHelper.saveAdditionalAclForFact(existingFact, request.getAcl())).thenReturn(request.getAcl());

    delegate.handle(request);

    verify(getFactManager()).refreshFact(existingFact.getId());
    verify(factStorageHelper).saveAdditionalAclForFact(same(existingFact), eq(request.getAcl()));
    verify(factStorageHelper).saveCommentForFact(same(existingFact), eq(request.getComment()));
    verify(getFactSearchManager()).indexFact(argThat(document -> document.getLastSeenTimestamp() > 0 &&
            Objects.equals(document.getAcl(), SetUtils.set(request.getAcl()))));
    verify(getFactManager(), never()).saveFact(any());
    verify(getFactConverter()).apply(same(existingFact));
  }

  private void mockCreateNewFact() throws Exception {
    mockValidator(true);
    mockFactConverter();
    mockFetchingOrganization();
    mockFetchingFactType();
    mockFetchingObjects();

    // Mock fetching of current user.
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());

    // Mock fetching of existing Fact.
    when(getFactSearchManager().retrieveExistingFacts(any())).thenReturn(SearchResult.<FactDocument>builder().build());

    // Mock stuff needed for saving Fact.
    when(getFactManager().saveFact(any())).thenAnswer(i -> i.getArgument(0));
    when(factStorageHelper.saveInitialAclForNewFact(any(), any())).thenAnswer(i -> i.getArgument(1));
  }

  private void mockFetchingOrganization() throws Exception {
    when(factCreateHelper.resolveOrigin(origin.getId())).thenReturn(origin);
    when(factCreateHelper.resolveOrganization(organization.getId(), origin)).thenReturn(organization);
  }

  private void mockFetchingFactType() throws Exception {
    when(factTypeResolver.resolveFactType(resolveFactType.getName())).thenReturn(resolveFactType);
    when(factTypeResolver.resolveFactType(retractionFactType.getName())).thenReturn(retractionFactType);
    when(factTypeResolver.resolveRetractionFactType()).thenReturn(retractionFactType);
  }

  private void mockFetchingObjects() throws Exception {
    when(getObjectManager().getObjectType(ipObjectType.getId())).thenReturn(ipObjectType);
    when(getObjectManager().getObjectType(domainObjectType.getId())).thenReturn(domainObjectType);

    when(getObjectManager().getObject(ip.getId())).thenReturn(ip);
    when(objectResolver.resolveObject(ip.getId().toString())).thenReturn(ip);

    when(getObjectManager().getObject(domain.getId())).thenReturn(domain);
    when(objectResolver.resolveObject(domain.getId().toString())).thenReturn(domain);

    when(getObjectManager().getObject(threatActor.getId())).thenReturn(threatActor);
    when(objectResolver.resolveObject(threatActor.getId().toString())).thenReturn(threatActor);
  }

  private Validator mockValidator(boolean valid) {
    Validator validator = mock(Validator.class);

    when(validator.validate(anyString())).thenReturn(valid);
    when(getValidatorFactory().get(anyString(), anyString())).thenReturn(validator);

    return validator;
  }

  private void mockFactConverter() {
    // Mock FactConverter needed for registering TriggerEvent.
    when(getFactConverter().apply(any())).then(i -> {
      FactEntity entity = i.getArgument(0);
      return Fact.builder()
              .setId(entity.getId())
              .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.valueOf(entity.getAccessMode().name()))
              .setOrganization(Organization.builder().setId(entity.getOrganizationID()).build().toInfo())
              .build();
    });
  }

  private CreateFactRequest createRequest() {
    return new CreateFactRequest()
            .setType(resolveFactType.getName())
            .setValue("factValue")
            .setOrganization(organization.getId())
            .setOrigin(origin.getId())
            .setConfidence(0.3f)
            .setComment("Hello World!")
            .setAccessMode(AccessMode.RoleBased)
            .setAcl(ListUtils.list(UUID.randomUUID()))
            .setSourceObject(ip.getId().toString())
            .setDestinationObject(domain.getId().toString());
  }

  private FactEntity matchFactEntity(CreateFactRequest request) {
    return argThat(entity -> {
      assertNotNull(entity.getId());
      assertEquals(resolveFactType.getId(), entity.getTypeID());
      assertEquals(request.getValue(), entity.getValue());
      assertEquals(request.getOrganization(), entity.getOrganizationID());
      assertNotNull(entity.getAddedByID());
      assertEquals(request.getOrigin(), entity.getOriginID());
      assertEquals(origin.getTrust(), entity.getTrust(), 0.0);
      assertEquals(request.getConfidence(), entity.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), entity.getAccessMode().name());
      assertTrue(entity.getTimestamp() > 0);
      assertTrue(entity.getLastSeenTimestamp() > 0);

      if (request.getSourceObject() != null) {
        assertTrue(entity.getBindings().stream()
                .anyMatch(b -> Objects.equals(b.getObjectID(), UUID.fromString(request.getSourceObject()))
                        && b.getDirection() == Direction.FactIsDestination));
      }

      if (request.getDestinationObject() != null) {
        assertTrue(entity.getBindings().stream()
                .anyMatch(b -> Objects.equals(b.getObjectID(), UUID.fromString(request.getDestinationObject()))
                        && b.getDirection() == Direction.FactIsSource));
      }

      return true;
    });
  }

  private FactDocument matchFactDocument(CreateFactRequest request) {
    return argThat(document -> {
      assertNotNull(document.getId());
      assertFalse(document.isRetracted());
      assertEquals(resolveFactType.getId(), document.getTypeID());
      assertEquals(resolveFactType.getName(), document.getTypeName());
      assertEquals(request.getValue(), document.getValue());
      assertEquals(request.getOrganization(), document.getOrganizationID());
      assertNotNull(document.getAddedByID());
      assertEquals(request.getOrigin(), document.getOriginID());
      assertEquals(origin.getTrust(), document.getTrust(), 0.0);
      assertEquals(request.getConfidence(), document.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), document.getAccessMode().name());
      assertTrue(document.getTimestamp() > 0);
      assertTrue(document.getLastSeenTimestamp() > 0);
      assertTrue(document.getAcl().size() > 0);

      if (request.getSourceObject() != null) {
        ObjectDocument source = document.getObjects()
                .stream()
                .filter(o -> Objects.equals(o.getId(), UUID.fromString(request.getSourceObject())))
                .findFirst()
                .orElse(null);
        assertNotNull(source);
        assertEquals(ipObjectType.getId(), source.getTypeID());
        assertEquals(ipObjectType.getName(), source.getTypeName());
        assertEquals(ip.getValue(), source.getValue());
        assertEquals(ObjectDocument.Direction.FactIsDestination, source.getDirection());
      }

      if (request.getDestinationObject() != null) {
        ObjectDocument destination = document.getObjects()
                .stream()
                .filter(o -> Objects.equals(o.getId(), UUID.fromString(request.getDestinationObject())))
                .findFirst()
                .orElse(null);
        assertNotNull(destination);
        assertEquals(domainObjectType.getId(), destination.getTypeID());
        assertEquals(domainObjectType.getName(), destination.getTypeName());
        assertEquals(domain.getValue(), destination.getValue());
        assertEquals(ObjectDocument.Direction.FactIsSource, destination.getDirection());
      }

      return true;
    });
  }

  private ObjectFactBindingEntity matchObjectFactBindingEntity() {
    return argThat(entity -> {
      assertNotNull(entity.getFactID());
      assertNotNull(entity.getObjectID());
      assertNotNull(entity.getDirection());
      return true;
    });
  }

  private FactExistenceSearchCriteria matchFactExistenceSearchCriteria(CreateFactRequest request) {
    return argThat(criteria -> {
      assertEquals(request.getValue(), criteria.getFactValue());
      assertEquals(resolveFactType.getId(), criteria.getFactTypeID());
      assertEquals(request.getOrigin(), criteria.getOriginID());
      assertEquals(request.getOrganization(), criteria.getOrganizationID());
      assertEquals(request.getAccessMode().name(), criteria.getAccessMode().name());
      assertEquals(request.getConfidence(), criteria.getConfidence(), 0.0);

      if (request.getSourceObject() != null) {
        assertTrue(criteria.getObjects().stream()
                .anyMatch(o -> Objects.equals(o.getObjectID(), UUID.fromString(request.getSourceObject()))
                        && o.getDirection() == FactExistenceSearchCriteria.Direction.FactIsDestination));
      }

      if (request.getDestinationObject() != null) {
        assertTrue(criteria.getObjects().stream()
                .anyMatch(o -> Objects.equals(o.getObjectID(), UUID.fromString(request.getDestinationObject()))
                        && o.getDirection() == FactExistenceSearchCriteria.Direction.FactIsSource));
      }

      return true;
    });
  }
}
