package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.api.request.v1.Direction;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.act.platform.service.ti.helpers.ObjectResolver;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.commons.testtools.MockitoTools.match;
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
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(any());
    delegate.handle(request);
  }

  @Test
  public void testCreateFactSetMissingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    CreateFactRequest request = mockCreateFact().setOrganization(null);

    when(getSecurityContext().getCurrentUserOrganizationID()).thenReturn(organizationID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(match(e -> organizationID.equals(e.getOrganizationID())));
    verify(getFactConverter()).apply(match(e -> organizationID.equals(e.getOrganizationID())));
  }

  @Test
  public void testCreateFactSetMissingSource() throws Exception {
    UUID sourceID = UUID.randomUUID();
    CreateFactRequest request = mockCreateFact().setSource(null);

    when(getSecurityContext().getCurrentUserID()).thenReturn(sourceID);

    delegate.handle(request);

    verify(getFactManager()).saveFact(match(e -> sourceID.equals(e.getSourceID())));
    verify(getFactConverter()).apply(match(e -> sourceID.equals(e.getSourceID())));
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
            .setAccessMode(no.mnemonic.act.platform.entity.cassandra.AccessMode.valueOf(request.getAccessMode().name()))
            .setLastSeenTimestamp(123)
            .setBindings(ListUtils.list(new FactEntity.FactObjectBinding()
                    .setObjectID(binding.getObjectID())
                    .setDirection(no.mnemonic.act.platform.entity.cassandra.Direction.valueOf(binding.getDirection().name()))
            ));

    when(getFactManager().fetchFactsByValue(request.getValue())).thenReturn(ListUtils.list(existingFact));
    when(getFactManager().refreshFact(existingFact.getId())).thenReturn(existingFact);

    delegate.handle(request);

    verify(getFactManager()).refreshFact(existingFact.getId());
    verify(factStorageHelper).saveAdditionalAclForFact(same(existingFact), eq(request.getAcl()));
    verify(factStorageHelper).saveCommentForFact(same(existingFact), eq(request.getComment()));
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

    // Mock stuff needed for saving Fact.
    when(getFactManager().getFact(request.getInReferenceTo())).thenReturn(new FactEntity());
    when(getFactManager().saveFact(any())).thenAnswer(i -> i.getArgument(0));

    return request;
  }

  private FactTypeEntity mockFetchingFactType(UUID objectTypeID) throws Exception {
    UUID id = UUID.randomUUID();
    String name = "factType";
    FactTypeEntity.FactObjectBindingDefinition definition = new FactTypeEntity.FactObjectBindingDefinition()
            .setObjectTypeID(objectTypeID)
            .setDirection(no.mnemonic.act.platform.entity.cassandra.Direction.BiDirectional);
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
    return match(entity -> {
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

  private ObjectFactBindingEntity matchObjectFactBindingEntity(CreateFactRequest.FactObjectBinding requestedBinding) {
    return match(entity -> {
      assertNotNull(entity.getFactID());
      assertEquals(requestedBinding.getObjectID(), entity.getObjectID());
      assertEquals(requestedBinding.getDirection().name(), entity.getDirection().name());
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
