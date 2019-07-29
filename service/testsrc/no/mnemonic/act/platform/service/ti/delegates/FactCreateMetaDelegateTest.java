package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.CreateMetaFactRequest;
import no.mnemonic.act.platform.dao.api.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.MetaFactBindingEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
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
  private FactTypeResolver factTypeResolver;
  @Mock
  private FactStorageHelper factStorageHelper;

  private FactCreateMetaDelegate delegate;

  private final FactTypeEntity retractionFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("Retraction");
  private FactTypeEntity seenInFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("seenIn");
  private FactTypeEntity observationFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("observation")
          .setValidator("validator")
          .setValidatorParameter("validatorParameter")
          .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(seenInFactType.getId()));
  private FactEntity seenIn = new FactEntity()
          .setId(UUID.randomUUID())
          .setTypeID(seenInFactType.getId())
          .setAccessMode(AccessMode.RoleBased);


  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactCreateMetaDelegate(
            getSecurityContext(),
            getTriggerContext(),
            getFactManager(),
            getFactSearchManager(),
            factTypeResolver,
            factStorageHelper,
            getFactConverter()
    );
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testCreateMetaFactReferencedFactNotExists() throws Exception {
    delegate.handle(createRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateMetaFactNoAccessToReferencedFact() throws Exception {
    when(getFactManager().getFact(seenIn.getId())).thenReturn(seenIn);
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(seenIn);

    delegate.handle(createRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateMetaFactWithoutAddPermission() throws Exception {
    CreateMetaFactRequest request = createRequest();

    when(getFactManager().getFact(seenIn.getId())).thenReturn(seenIn);
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addFactObjects, request.getOrganization());

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateMetaFactWithRetractionFactTypeThrowsException() throws Exception {
    CreateMetaFactRequest request = new CreateMetaFactRequest()
            .setFact(seenIn.getId())
            .setType(retractionFactType.getName());

    when(getFactManager().getFact(seenIn.getId())).thenReturn(seenIn);
    mockFetchingFactType();

    delegate.handle(request);
  }

  @Test
  public void testValidateFactValueThrowsException() throws Exception {
    CreateMetaFactRequest request = createRequest();

    when(getFactManager().getFact(seenIn.getId())).thenReturn(seenIn);
    mockFetchingFactType();
    Validator validatorMock = mockValidator(false);

    expectInvalidArgumentException(() -> delegate.handle(request), "fact.not.valid");

    verify(validatorMock).validate(request.getValue());
  }

  @Test
  public void testValidateBindingFailsWithoutBindingsOnFactType() throws Exception {
    CreateMetaFactRequest request = createRequest();
    observationFactType.setRelevantFactBindings(null);

    when(getFactManager().getFact(seenIn.getId())).thenReturn(seenIn);
    mockFetchingFactType();
    mockValidator(true);

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.meta.fact.binding");
  }

  @Test
  public void testValidateBindingFailsOnType() throws Exception {
    FactEntity anotherFact = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID());
    CreateMetaFactRequest request = createRequest()
            .setFact(anotherFact.getId());

    when(getFactManager().getFact(anotherFact.getId())).thenReturn(anotherFact);
    mockFetchingFactType();
    mockValidator(true);

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.meta.fact.binding");
  }

  @Test
  public void testCreateMetaFact() throws Exception {
    CreateMetaFactRequest request = createRequest();
    mockCreateNewFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(matchFactEntity(request));
    verify(getFactManager()).saveMetaFactBinding(matchMetaFactBindingEntity());
    verify(factStorageHelper).saveInitialAclForNewFact(matchFactEntity(request), eq(request.getAcl()));
    verify(factStorageHelper).saveCommentForFact(matchFactEntity(request), eq(request.getComment()));
    verify(getFactSearchManager()).indexFact(matchFactDocument(request));
    verify(getFactConverter()).apply(matchFactEntity(request));
  }

  @Test
  public void testCreateMetaFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    CreateMetaFactRequest request = createRequest()
            .setOrganization(null);
    mockCreateNewFact();

    when(getSecurityContext().getCurrentUserOrganizationID()).thenReturn(organizationID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> organizationID.equals(e.getOrganizationID())));
    verify(getFactConverter()).apply(argThat(e -> organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testCreateMetaFactSetMissingSource() throws Exception {
    UUID sourceID = UUID.randomUUID();
    CreateMetaFactRequest request = createRequest()
            .setSource(null);
    mockCreateNewFact();

    when(getSecurityContext().getCurrentUserID()).thenReturn(sourceID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> sourceID.equals(e.getSourceID())));
    verify(getFactConverter()).apply(argThat(e -> sourceID.equals(e.getSourceID())));
  }

  @Test
  public void testCreateMetaFactSetMissingAccessMode() throws Exception {
    CreateMetaFactRequest request = createRequest()
            .setAccessMode(null);
    mockCreateNewFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> seenIn.getAccessMode().equals(e.getAccessMode())));
    verify(getFactConverter()).apply(argThat(e -> seenIn.getAccessMode().equals(e.getAccessMode())));
  }

  @Test
  public void testCreateMetaFactWithLessRestrictiveAccessMode() throws Exception {
    CreateMetaFactRequest request = createRequest()
            .setAccessMode(no.mnemonic.act.platform.api.request.v1.AccessMode.Public);
    mockCreateNewFact();

    expectInvalidArgumentException(() -> delegate.handle(request), "access.mode.too.wide");

    verify(getFactManager(), never()).saveFact(any());
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

    FactEntity existingFact = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(observationFactType.getId())
            .setValue(request.getValue())
            .setInReferenceToID(request.getFact())
            .setSourceID(request.getSource())
            .setOrganizationID(request.getOrganization())
            .setAccessMode(AccessMode.valueOf(request.getAccessMode().name()))
            .setLastSeenTimestamp(123);

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

  private CreateMetaFactRequest createRequest() {
    return new CreateMetaFactRequest()
            .setFact(seenIn.getId())
            .setType(observationFactType.getName())
            .setValue("today")
            .setOrganization(UUID.randomUUID())
            .setSource(UUID.randomUUID())
            .setAccessMode(no.mnemonic.act.platform.api.request.v1.AccessMode.Explicit)
            .setComment("Hello World!")
            .addAcl(UUID.randomUUID());
  }

  private void mockCreateNewFact() throws Exception {
    mockFetchingFactType();
    mockValidator(true);
    mockFactConverter();

    // Mock fetching of referenced Fact.
    when(getFactManager().getFact(seenIn.getId())).thenReturn(seenIn);
    // Mock fetching of existing Fact.
    when(getFactSearchManager().retrieveExistingFacts(any())).thenReturn(SearchResult.<FactDocument>builder().build());
    // Mock stuff needed for saving Fact.
    when(getFactManager().saveFact(any())).thenAnswer(i -> i.getArgument(0));
    when(factStorageHelper.saveInitialAclForNewFact(any(), any())).thenAnswer(i -> i.getArgument(1));
  }

  private void mockFetchingFactType() throws Exception {
    when(factTypeResolver.resolveFactType(observationFactType.getName())).thenReturn(observationFactType);
    when(factTypeResolver.resolveFactType(retractionFactType.getName())).thenReturn(retractionFactType);
    when(factTypeResolver.resolveRetractionFactType()).thenReturn(retractionFactType);
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

  private FactEntity matchFactEntity(CreateMetaFactRequest request) {
    return argThat(entity -> {
      assertNotNull(entity.getId());
      assertEquals(observationFactType.getId(), entity.getTypeID());
      assertEquals(request.getValue(), entity.getValue());
      assertEquals(request.getFact(), entity.getInReferenceToID());
      assertEquals(request.getOrganization(), entity.getOrganizationID());
      assertEquals(request.getSource(), entity.getSourceID());
      assertEquals(request.getAccessMode().name(), entity.getAccessMode().name());
      assertTrue(entity.getTimestamp() > 0);
      assertTrue(entity.getLastSeenTimestamp() > 0);

      return true;
    });
  }

  private FactDocument matchFactDocument(CreateMetaFactRequest request) {
    return argThat(document -> {
      assertNotNull(document.getId());
      assertFalse(document.isRetracted());
      assertEquals(observationFactType.getId(), document.getTypeID());
      assertEquals(observationFactType.getName(), document.getTypeName());
      assertEquals(request.getValue(), document.getValue());
      assertEquals(request.getFact(), document.getInReferenceTo());
      assertEquals(request.getOrganization(), document.getOrganizationID());
      assertEquals(request.getSource(), document.getSourceID());
      assertEquals(request.getAccessMode().name(), document.getAccessMode().name());
      assertTrue(document.getTimestamp() > 0);
      assertTrue(document.getLastSeenTimestamp() > 0);
      assertTrue(document.getAcl().size() > 0);

      return true;
    });
  }

  private MetaFactBindingEntity matchMetaFactBindingEntity() {
    return argThat(entity -> {
      assertNotNull(entity.getFactID());
      assertNotNull(entity.getMetaFactID());
      return true;
    });
  }

  private FactExistenceSearchCriteria matchFactExistenceSearchCriteria(CreateMetaFactRequest request) {
    return argThat(criteria -> {
      assertEquals(request.getValue(), criteria.getFactValue());
      assertEquals(observationFactType.getId(), criteria.getFactTypeID());
      assertEquals(request.getSource(), criteria.getSourceID());
      assertEquals(request.getOrganization(), criteria.getOrganizationID());
      assertEquals(request.getAccessMode().name(), criteria.getAccessMode().name());
      assertEquals(request.getFact(), criteria.getInReferenceTo());

      return true;
    });
  }
}
