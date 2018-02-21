package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.api.request.v1.Direction;
import no.mnemonic.act.platform.dao.api.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.act.platform.service.ti.helpers.ObjectResolver;
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
  private FactStorageHelper factStorageHelper;

  private FactCreateDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = FactCreateDelegate.builder()
            .setFactTypeResolver(factTypeResolver)
            .setObjectResolver(objectResolver)
            .setFactStorageHelper(factStorageHelper)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactTypeResolver() {
    FactCreateDelegate.builder()
            .setObjectResolver(objectResolver)
            .setFactStorageHelper(factStorageHelper)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutObjectResolver() {
    FactCreateDelegate.builder()
            .setFactTypeResolver(factTypeResolver)
            .setFactStorageHelper(factStorageHelper)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactStorageHelper() {
    FactCreateDelegate.builder()
            .setObjectResolver(objectResolver)
            .setFactTypeResolver(factTypeResolver)
            .build();
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactWithoutAddPermission() throws Exception {
    CreateFactRequest request = createRequest();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addFactObjects, request.getOrganization());
    delegate.handle(request);
  }

  @Test
  public void testValidateFactValueThrowsException() throws Exception {
    mockFetchingFactType(UUID.randomUUID());
    Validator validatorMock = mockValidator(false);
    CreateFactRequest request = createRequest();

    expectInvalidArgumentException(() -> delegate.handle(request), "fact.not.valid");

    verify(validatorMock).validate(request.getValue());
  }

  @Test
  public void testValidateBindingsFailsOnType() throws Exception {
    mockFetchingFactType(UUID.randomUUID());
    mockFetchingObject(UUID.randomUUID(), UUID.randomUUID()); // Provide different type.
    mockValidator(true);

    CreateFactRequest.FactObjectBinding binding = createBindingRequest();
    CreateFactRequest request = createRequest().addBinding(binding);

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.fact.object.binding");
  }

  @Test
  public void testValidateBindingsFailsOnDirection() throws Exception {
    UUID objectTypeID = UUID.randomUUID();
    mockFetchingFactType(objectTypeID);
    mockFetchingObject(UUID.randomUUID(), objectTypeID);
    mockValidator(true);

    CreateFactRequest.FactObjectBinding binding = createBindingRequest()
            .setDirection(Direction.FactIsDestination); // Provide different direction.
    CreateFactRequest request = createRequest().addBinding(binding);

    expectInvalidArgumentException(() -> delegate.handle(request), "invalid.fact.object.binding");
  }

  @Test
  public void testCreateFact() throws Exception {
    CreateFactRequest request = mockCreateFact();

    delegate.handle(request);

    verify(getFactManager()).saveFact(matchFactEntity(request));
    verify(getObjectManager()).saveObjectFactBinding(matchObjectFactBindingEntity(request.getBindings().get(0)));
    verify(factStorageHelper).saveInitialAclForNewFact(matchFactEntity(request), eq(request.getAcl()));
    verify(factStorageHelper).saveCommentForFact(matchFactEntity(request), eq(request.getComment()));
    verify(getFactSearchManager()).indexFact(matchFactDocument(request));
    verify(getFactConverter()).apply(matchFactEntity(request));
  }

  @Test
  public void testCreateFactFailsOnMissingInReferenceTo() throws Exception {
    CreateFactRequest request = mockCreateFact().setInReferenceTo(UUID.randomUUID()); // Provide different 'inReferenceTo' Fact.
    expectInvalidArgumentException(() -> delegate.handle(request), "referenced.fact.not.exist");
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactFailsNoAccessToInReferenceTo() throws Exception {
    CreateFactRequest request = mockCreateFact();
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactEntity.class));
    delegate.handle(request);
  }

  @Test
  public void testCreateFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    CreateFactRequest request = mockCreateFact().setOrganization(null);

    when(getSecurityContext().getCurrentUserOrganizationID()).thenReturn(organizationID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> organizationID.equals(e.getOrganizationID())));
    verify(getFactConverter()).apply(argThat(e -> organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testCreateFactSetMissingSource() throws Exception {
    UUID sourceID = UUID.randomUUID();
    CreateFactRequest request = mockCreateFact().setSource(null);

    when(getSecurityContext().getCurrentUserID()).thenReturn(sourceID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(argThat(e -> sourceID.equals(e.getSourceID())));
    verify(getFactConverter()).apply(argThat(e -> sourceID.equals(e.getSourceID())));
  }

  @Test
  public void testRefreshExistingFact() throws Exception {
    CreateFactRequest.FactObjectBinding binding = createBindingRequest();
    CreateFactRequest request = createRequest().addBinding(binding);

    // Mock stuff needed for validation.
    UUID objectTypeID = UUID.randomUUID();
    FactTypeEntity type = mockFetchingFactType(objectTypeID);
    mockFetchingObject(binding.getObjectID(), objectTypeID);
    mockValidator(true);

    FactEntity existingFact = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(type.getId())
            .setValue(request.getValue())
            .setSourceID(request.getSource())
            .setOrganizationID(request.getOrganization())
            .setAccessMode(no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.valueOf(request.getAccessMode().name()))
            .setLastSeenTimestamp(123)
            .setBindings(ListUtils.list(new FactEntity.FactObjectBinding()
                    .setObjectID(binding.getObjectID())
                    .setDirection(no.mnemonic.act.platform.dao.cassandra.entity.Direction.valueOf(binding.getDirection().name()))
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

  private CreateFactRequest mockCreateFact() throws Exception {
    // Create request object.
    CreateFactRequest.FactObjectBinding binding = createBindingRequest();
    CreateFactRequest request = createRequest().addBinding(binding);

    // Mock stuff needed for validation.
    UUID objectTypeID = UUID.randomUUID();
    mockFetchingFactType(objectTypeID);
    mockFetchingObject(binding.getObjectID(), objectTypeID);
    mockValidator(true);

    // Mock fetching of existing Fact.
    when(getFactSearchManager().retrieveExistingFacts(any())).thenReturn(SearchResult.<FactDocument>builder().build());

    // Mock stuff needed for saving Fact.
    when(getFactManager().getFact(request.getInReferenceTo())).thenReturn(new FactEntity());
    when(getFactManager().saveFact(any())).thenAnswer(i -> i.getArgument(0));
    when(factStorageHelper.saveInitialAclForNewFact(any(), any())).thenAnswer(i -> i.getArgument(1));

    return request;
  }

  private FactTypeEntity mockFetchingFactType(UUID objectTypeID) throws Exception {
    UUID id = UUID.randomUUID();
    String name = "factType";
    FactTypeEntity.FactObjectBindingDefinition definition = new FactTypeEntity.FactObjectBindingDefinition()
            .setObjectTypeID(objectTypeID)
            .setDirection(no.mnemonic.act.platform.dao.cassandra.entity.Direction.BiDirectional);
    FactTypeEntity type = new FactTypeEntity()
            .setId(id)
            .setName(name)
            .setValidator("validator")
            .setValidatorParameter("validatorParameter")
            .setRelevantObjectBindings(ListUtils.list(definition));

    when(factTypeResolver.resolveFactType(name)).thenReturn(type);

    return type;
  }

  private void mockFetchingObject(UUID id, UUID objectTypeID) throws Exception {
    ObjectEntity object = new ObjectEntity()
            .setId(id)
            .setTypeID(objectTypeID)
            .setValue("objectValue");

    when(getObjectManager().getObject(id)).thenReturn(object);
    when(getObjectManager().getObjectType(objectTypeID)).thenReturn(new ObjectTypeEntity().setId(objectTypeID).setName("objectType"));
    when(objectResolver.resolveObject(any(), any(), any())).thenReturn(object);
  }

  private Validator mockValidator(boolean valid) {
    Validator validator = mock(Validator.class);

    when(validator.validate(anyString())).thenReturn(valid);
    when(getValidatorFactory().get(anyString(), anyString())).thenReturn(validator);

    return validator;
  }

  private CreateFactRequest createRequest() {
    return new CreateFactRequest()
            .setType("factType")
            .setValue("factValue")
            .setInReferenceTo(UUID.randomUUID())
            .setOrganization(UUID.randomUUID())
            .setSource(UUID.randomUUID())
            .setComment("Hello World!")
            .setAccessMode(AccessMode.RoleBased)
            .setAcl(ListUtils.list(UUID.randomUUID()));
  }

  private CreateFactRequest.FactObjectBinding createBindingRequest() {
    return new CreateFactRequest.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setObjectType("objectType")
            .setObjectValue("objectValue")
            .setDirection(Direction.BiDirectional);
  }

  private FactEntity matchFactEntity(CreateFactRequest request) {
    return argThat(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getTypeID());
      assertEquals(request.getValue(), entity.getValue());
      assertEquals(request.getInReferenceTo(), entity.getInReferenceToID());
      assertEquals(request.getOrganization(), entity.getOrganizationID());
      assertEquals(request.getSource(), entity.getSourceID());
      assertEquals(request.getAccessMode().name(), entity.getAccessMode().name());
      assertTrue(entity.getTimestamp() > 0);
      assertTrue(entity.getLastSeenTimestamp() > 0);
      assertEquals(request.getBindings().size(), entity.getBindings().size());
      assertEquals(request.getBindings().get(0).getObjectID(), entity.getBindings().get(0).getObjectID());
      assertEquals(request.getBindings().get(0).getDirection().name(), entity.getBindings().get(0).getDirection().name());
      return true;
    });
  }

  private FactDocument matchFactDocument(CreateFactRequest request) {
    return argThat(document -> {
      assertNotNull(document.getId());
      assertFalse(document.isRetracted());
      assertNotNull(document.getTypeID());
      assertEquals("factType", document.getTypeName());
      assertEquals(request.getValue(), document.getValue());
      assertEquals(request.getInReferenceTo(), document.getInReferenceTo());
      assertEquals(request.getOrganization(), document.getOrganizationID());
      assertEquals(request.getSource(), document.getSourceID());
      assertEquals(request.getAccessMode().name(), document.getAccessMode().name());
      assertTrue(document.getTimestamp() > 0);
      assertTrue(document.getLastSeenTimestamp() > 0);
      assertTrue(document.getAcl().size() > 0);
      assertEquals(request.getBindings().size(), document.getObjects().size());
      assertEquals(request.getBindings().get(0).getObjectID(), document.getObjects().iterator().next().getId());
      assertNotNull(document.getObjects().iterator().next().getTypeID());
      assertEquals("objectType", document.getObjects().iterator().next().getTypeName());
      assertEquals("objectValue", document.getObjects().iterator().next().getValue());
      assertEquals(request.getBindings().get(0).getDirection().name(), document.getObjects().iterator().next().getDirection().name());
      return true;
    });
  }

  private ObjectFactBindingEntity matchObjectFactBindingEntity(CreateFactRequest.FactObjectBinding requestedBinding) {
    return argThat(entity -> {
      assertNotNull(entity.getFactID());
      assertEquals(requestedBinding.getObjectID(), entity.getObjectID());
      assertEquals(requestedBinding.getDirection().name(), entity.getDirection().name());
      return true;
    });
  }

  private FactExistenceSearchCriteria matchFactExistenceSearchCriteria(CreateFactRequest request) {
    return argThat(criteria -> {
      assertEquals(request.getValue(), criteria.getFactValue());
      assertNotNull(criteria.getFactTypeID());
      assertEquals(request.getSource(), criteria.getSourceID());
      assertEquals(request.getOrganization(), criteria.getOrganizationID());
      assertEquals(request.getAccessMode().name(), criteria.getAccessMode().name());
      assertEquals(request.getBindings().size(), criteria.getObjects().size());
      assertEquals(request.getBindings().get(0).getObjectID(), criteria.getObjects().iterator().next().getObjectID());
      assertEquals(request.getBindings().get(0).getDirection().name(), criteria.getObjects().iterator().next().getDirection().name());
      return true;
    });
  }

  private void expectInvalidArgumentException(InvalidArgumentExceptionTest test, String messageTemplate) throws Exception {
    try {
      test.execute();
      fail();
    } catch (InvalidArgumentException ex) {
      assertEquals(1, ex.getValidationErrors().size());
      assertEquals(messageTemplate, ex.getValidationErrors().iterator().next().getMessageTemplate());
    }
  }

  private interface InvalidArgumentExceptionTest {
    void execute() throws Exception;
  }

}
