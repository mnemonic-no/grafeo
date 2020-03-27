package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.act.platform.service.ti.resolvers.ObjectResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Objects;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactCreateDelegateTest {

  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private ObjectResolver objectResolver;
  @Mock
  private FactCreateHandler factCreateHandler;
  @Mock
  private TiSecurityContext securityContext;

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
      factTypeResolver,
      objectResolver,
      factCreateHandler
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactWithoutAddPermission() throws Exception {
    mockFetchingOrganization();
    mockFetchingFactType();
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.addFactObjects, organization.getId());
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

    verify(objectResolver, times(2)).resolveObject("unknown");
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

    verify(factCreateHandler).saveFact(matchFactRecord(request), eq(request.getComment()), eq(request.getAcl()));
  }

  @Test
  public void testCreateFactWithOnlyDestinationObject() throws Exception {
    CreateFactRequest request = createRequest()
            .setSourceObject(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), eq(request.getComment()), eq(request.getAcl()));
  }

  @Test
  public void testCreateFactWithBothSourceAndDestinationObjects() throws Exception {
    CreateFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(matchFactRecord(request), eq(request.getComment()), eq(request.getAcl()));
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

    verify(factCreateHandler).saveFact(matchFactRecord(request), eq(request.getComment()), eq(request.getAcl()));
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

    verify(factCreateHandler).saveFact(
      argThat(e -> organizationID.equals(e.getOrganizationID())),
      eq(request.getComment()),
      eq(request.getAcl()));
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

    verify(factCreateHandler).saveFact(
      argThat(e -> originID.equals(e.getOriginID())),
      eq(request.getComment()),
      eq(request.getAcl())
    );
  }

  @Test
  public void testCreateFactSetMissingConfidence() throws Exception {
    CreateFactRequest request = createRequest()
            .setConfidence(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).saveFact(
      argThat(e -> Objects.equals(resolveFactType.getDefaultConfidence(), e.getConfidence())),
      eq(request.getComment()),
      eq(request.getAcl())
    );
  }

  private void mockCreateNewFact() throws Exception {
    mockFetchingOrganization();
    mockFetchingFactType();
    mockFetchingObjects();

    // Mock fetching of current user.
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
  }

  private void mockFetchingOrganization() throws Exception {
    when(factCreateHandler.resolveOrigin(origin.getId())).thenReturn(origin);
    when(factCreateHandler.resolveOrganization(organization.getId(), origin)).thenReturn(organization);
  }

  private void mockFetchingFactType() throws Exception {
    when(factTypeResolver.resolveFactType(resolveFactType.getName())).thenReturn(resolveFactType);
  }

  private void mockFetchingObjects() throws Exception {
    when(objectResolver.resolveObject(ip.getId().toString())).thenReturn(ip);
    when(objectResolver.resolveObject(domain.getId().toString())).thenReturn(domain);
    when(objectResolver.resolveObject(threatActor.getId().toString())).thenReturn(threatActor);
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
