package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.FactConverter;
import no.mnemonic.act.platform.service.ti.helpers.FactCreateHelper;
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
  private ObjectFactDao objectFactDao;
  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private ObjectResolver objectResolver;
  @Mock
  private FactCreateHelper factCreateHelper;
  @Mock
  private FactConverter factConverter;

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
    // initMocks() will be called by base class.
    delegate = new FactCreateDelegate(
            getSecurityContext(),
            getTriggerContext(),
            objectFactDao,
            factTypeResolver,
            objectResolver,
            factCreateHelper,
            factConverter
    );
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

    verify(objectFactDao).storeFact(matchFactRecord(request));
    verify(factConverter).apply(matchFactRecord(request));
  }

  @Test
  public void testCreateFactWithOnlyDestinationObject() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(objectFactDao).storeFact(matchFactRecord(request));
    verify(factConverter).apply(matchFactRecord(request));
  }

  @Test
  public void testCreateFactWithBothSourceAndDestinationObjects() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(objectFactDao).storeFact(matchFactRecord(request));
    verify(factConverter).apply(matchFactRecord(request));
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

    verify(objectFactDao).storeFact(matchFactRecord(request));
    verify(factConverter).apply(matchFactRecord(request));
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

    verify(objectFactDao).storeFact(argThat(e -> organizationID.equals(e.getOrganizationID())));
    verify(factConverter).apply(argThat(e -> organizationID.equals(e.getOrganizationID())));
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

    verify(objectFactDao).storeFact(argThat(e -> originID.equals(e.getOriginID())));
    verify(factConverter).apply(argThat(e -> originID.equals(e.getOriginID())));
  }

  @Test
  public void testCreateFactSetMissingConfidence() throws Exception {
    CreateFactRequest request = createRequest()
            .setConfidence(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> Objects.equals(resolveFactType.getDefaultConfidence(), e.getConfidence())));
    verify(factConverter).apply(argThat(e -> Objects.equals(resolveFactType.getDefaultConfidence(), e.getConfidence())));
  }

  @Test
  public void testCreateFactStoresAclAndComment() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHelper).withComment(matchFactRecord(request), eq(request.getComment()));
    verify(factCreateHelper).withAcl(matchFactRecord(request), eq(request.getAcl()));
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

    // Mock fetching of existing Fact.
    FactRecord existingFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .setOrganizationID(request.getOrganization());
    when(objectFactDao.retrieveExistingFacts(matchFactRecord(request)))
            .thenReturn(ResultContainer.<FactRecord>builder().setValues(ListUtils.list(existingFact).iterator()).build());
    when(getSecurityContext().hasReadPermission(existingFact)).thenReturn(true);

    // Mock stuff needed for refreshing Fact.
    when(objectFactDao.refreshFact(existingFact)).thenReturn(existingFact);

    delegate.handle(request);

    verify(factCreateHelper).withComment(same(existingFact), eq(request.getComment()));
    verify(factCreateHelper).withAcl(same(existingFact), eq(request.getAcl()));
    verify(objectFactDao).refreshFact(existingFact);
    verify(objectFactDao, never()).storeFact(any());
    verify(factConverter).apply(same(existingFact));
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
    when(objectFactDao.retrieveExistingFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    // Mock stuff needed for saving Fact.
    when(objectFactDao.storeFact(any())).thenAnswer(i -> i.getArgument(0));
    when(factCreateHelper.withAcl(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(factCreateHelper.withComment(any(), any())).thenAnswer(i -> i.getArgument(0));
  }

  private void mockFetchingOrganization() throws Exception {
    when(factCreateHelper.resolveOrigin(origin.getId())).thenReturn(origin);
    when(factCreateHelper.resolveOrganization(organization.getId(), origin)).thenReturn(organization);
  }

  private void mockFetchingFactType() throws Exception {
    when(factTypeResolver.resolveFactType(resolveFactType.getName())).thenReturn(resolveFactType);
  }

  private void mockFetchingObjects() throws Exception {
    when(objectResolver.resolveObject(ip.getId().toString())).thenReturn(ip);
    when(objectResolver.resolveObject(domain.getId().toString())).thenReturn(domain);
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
    when(factConverter.apply(any())).then(i -> {
      FactRecord record = i.getArgument(0);
      return Fact.builder()
              .setId(record.getId())
              .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.valueOf(record.getAccessMode().name()))
              .setOrganization(Organization.builder().setId(record.getOrganizationID()).build().toInfo())
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

  private FactRecord matchFactRecord(CreateFactRequest request) {
    return argThat(record -> {
      assertNotNull(record.getId());
      assertEquals(resolveFactType.getId(), record.getTypeID());
      assertEquals(request.getValue(), record.getValue());
      assertEquals(request.getOrganization(), record.getOrganizationID());
      assertNotNull(record.getAddedByID());
      assertEquals(request.getOrigin(), record.getOriginID());
      assertEquals(origin.getTrust(), record.getTrust(), 0.0);
      assertEquals(request.getConfidence(), record.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), record.getAccessMode().name());
      assertTrue(record.getTimestamp() > 0);
      assertTrue(record.getLastSeenTimestamp() > 0);
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
