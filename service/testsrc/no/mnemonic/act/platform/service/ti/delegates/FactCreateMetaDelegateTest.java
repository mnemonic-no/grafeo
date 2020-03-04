package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.CreateMetaFactRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.FactConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class FactCreateMetaDelegateTest extends AbstractDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private FactResolver factResolver;
  @Mock
  private FactCreateHandler factCreateHandler;
  @Mock
  private FactConverter factConverter;

  private FactCreateMetaDelegate delegate;

  private final OriginEntity origin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setName("origin")
          .setTrust(0.1f);
  private final Organization organization = Organization.builder()
          .setId(UUID.randomUUID())
          .setName("organization")
          .build();
  private FactTypeEntity seenInFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("seenIn");
  private FactTypeEntity observationFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("observation")
          .setValidator("validator")
          .setValidatorParameter("validatorParameter")
          .setDefaultConfidence(0.2f)
          .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(seenInFactType.getId()));
  private FactRecord seenIn = new FactRecord()
          .setId(UUID.randomUUID())
          .setTypeID(seenInFactType.getId())
          .setAccessMode(FactRecord.AccessMode.RoleBased);


  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactCreateMetaDelegate(
            getSecurityContext(),
            getTriggerContext(),
            objectFactDao,
            factTypeResolver,
            factResolver,
      factCreateHandler,
            factConverter
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateMetaFactNoAccessToReferencedFact() throws Exception {
    when(factResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(seenIn);

    delegate.handle(createRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateMetaFactWithoutAddPermission() throws Exception {
    when(factResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    mockFetchingOrganization();
    mockFetchingFactType();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addFactObjects, organization.getId());
    delegate.handle(createRequest());
  }

  @Test
  public void testFactValueIsValidated() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).assertValidFactValue(any(), eq(request.getValue()));
  }

  @Test
  public void testValidateBindingFailsWithoutBindingsOnFactType() throws Exception {
    CreateMetaFactRequest request = createRequest();
    observationFactType.setRelevantFactBindings(null);

    when(factResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    mockFetchingOrganization();
    mockFetchingFactType();
    mockValidator(true);

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(SetUtils.set("invalid.meta.fact.binding"), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidateBindingFailsOnType() throws Exception {
    FactRecord anotherFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID());
    CreateMetaFactRequest request = createRequest()
            .setFact(anotherFact.getId());

    when(factResolver.resolveFact(anotherFact.getId())).thenReturn(anotherFact);
    mockFetchingOrganization();
    mockFetchingFactType();
    mockValidator(true);

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(SetUtils.set("invalid.meta.fact.binding"), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testCreateMetaFact() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(objectFactDao).storeFact(matchFactRecord(request));
    verify(factConverter).apply(matchFactRecord(request));
  }

  @Test
  public void testCreateMetaFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    CreateMetaFactRequest request = createRequest()
            .setOrganization(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveOrganization(isNull(), eq(origin)))
            .thenReturn(Organization.builder().setId(organizationID).build());

    delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> organizationID.equals(e.getOrganizationID())));
    verify(factConverter).apply(argThat(e -> organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testCreateMetaFactSetMissingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    CreateMetaFactRequest request = createRequest()
            .setOrigin(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveOrigin(isNull())).thenReturn(new OriginEntity().setId(originID));
    when(factCreateHandler.resolveOrganization(notNull(), notNull())).thenReturn(organization);

    delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> originID.equals(e.getOriginID())));
    verify(factConverter).apply(argThat(e -> originID.equals(e.getOriginID())));
  }

  @Test
  public void testCreateMetaFactSetMissingConfidence() throws Exception {
    CreateMetaFactRequest request = createRequest()
            .setConfidence(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> Objects.equals(observationFactType.getDefaultConfidence(), e.getConfidence())));
    verify(factConverter).apply(argThat(e -> Objects.equals(observationFactType.getDefaultConfidence(), e.getConfidence())));
  }

  @Test
  public void testCreateMetaFactSetMissingAccessMode() throws Exception {
    CreateMetaFactRequest request = createRequest()
            .setAccessMode(null);
    mockCreateNewFact();

    when(factCreateHandler.resolveAccessMode(eq(seenIn), isNull())).thenReturn(seenIn.getAccessMode());

    delegate.handle(request);

    verify(objectFactDao).storeFact(argThat(e -> seenIn.getAccessMode().equals(e.getAccessMode())));
    verify(factConverter).apply(argThat(e -> seenIn.getAccessMode().equals(e.getAccessMode())));
  }

  @Test
  public void testCreateFactStoresAclAndComment() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(factCreateHandler).withComment(matchFactRecord(request), eq(request.getComment()));
    verify(factCreateHandler).withAcl(matchFactRecord(request), eq(request.getAcl()));
  }

  @Test
  public void testCreateMetaFactRegistersTriggerEvent() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    Fact addedFact = delegate.handle(request);

    verify(getTriggerContext()).registerTriggerEvent(argThat(event -> {
      assertNotNull(event);
      assertEquals(TiServiceEvent.EventName.FactAdded.name(), event.getEvent());
      assertEquals(request.getOrganization(), event.getOrganization());
      assertEquals("Private", event.getAccessMode().name());
      assertSame(addedFact, event.getContextParameters().get(TiServiceEvent.ContextParameter.AddedFact.name()));
      return true;
    }));
  }

  @Test
  public void testRefreshExistingMetaFact() throws Exception {
    CreateMetaFactRequest request = createRequest();
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

    verify(factCreateHandler).withComment(same(existingFact), eq(request.getComment()));
    verify(factCreateHandler).withAcl(same(existingFact), eq(request.getAcl()));
    verify(objectFactDao).refreshFact(existingFact);
    verify(objectFactDao, never()).storeFact(any());
    verify(factConverter).apply(same(existingFact));
  }

  private CreateMetaFactRequest createRequest() {
    return new CreateMetaFactRequest()
            .setFact(seenIn.getId())
            .setType(observationFactType.getName())
            .setValue("today")
            .setOrganization(organization.getId())
            .setOrigin(origin.getId())
            .setConfidence(0.3f)
            .setAccessMode(AccessMode.Explicit)
            .setComment("Hello World!")
            .addAcl(UUID.randomUUID());
  }

  private void mockCreateNewFact() throws Exception {
    mockFetchingFactType();
    mockValidator(true);
    mockFactConverter();
    mockFetchingOrganization();

    // Mock fetching of current user.
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    // Mock fetching of referenced Fact.
    when(factResolver.resolveFact(seenIn.getId())).thenReturn(seenIn);
    when(factCreateHandler.resolveAccessMode(eq(seenIn), any())).thenReturn(FactRecord.AccessMode.Explicit);
    // Mock fetching of existing Fact.
    when(objectFactDao.retrieveExistingFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    // Mock stuff needed for saving Fact.
    when(objectFactDao.storeFact(any())).thenAnswer(i -> i.getArgument(0));
    when(factCreateHandler.withAcl(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(factCreateHandler.withComment(any(), any())).thenAnswer(i -> i.getArgument(0));
  }

  private void mockFetchingOrganization() throws Exception {
    when(factCreateHandler.resolveOrigin(origin.getId())).thenReturn(origin);
    when(factCreateHandler.resolveOrganization(organization.getId(), origin)).thenReturn(organization);
  }

  private void mockFetchingFactType() throws Exception {
    when(factTypeResolver.resolveFactType(observationFactType.getName())).thenReturn(observationFactType);
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

  private FactRecord matchFactRecord(CreateMetaFactRequest request) {
    return argThat(record -> {
      assertNotNull(record.getId());
      assertEquals(observationFactType.getId(), record.getTypeID());
      assertEquals(request.getValue(), record.getValue());
      assertEquals(request.getFact(), record.getInReferenceToID());
      assertEquals(request.getOrganization(), record.getOrganizationID());
      assertNotNull(record.getAddedByID());
      assertEquals(request.getOrigin(), record.getOriginID());
      assertEquals(origin.getTrust(), record.getTrust(), 0.0);
      assertEquals(request.getConfidence(), record.getConfidence(), 0.0);
      assertEquals(request.getAccessMode().name(), record.getAccessMode().name());
      assertTrue(record.getTimestamp() > 0);
      assertTrue(record.getLastSeenTimestamp() > 0);

      return true;
    });
  }
}
