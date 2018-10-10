package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FactTypeCreateDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addTypes);
    FactTypeCreateDelegate.create().handle(createRequest());
  }

  @Test
  public void testCreateAlreadyExistingFactType() throws Exception {
    CreateFactTypeRequest request = createRequest();
    when(getFactManager().getFactType(request.getName())).thenReturn(new FactTypeEntity());
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    expectInvalidArgumentException(() -> FactTypeCreateDelegate.create().handle(request), "fact.type.exist");
  }

  @Test
  public void testCreateFactTypeWithoutSpecifiedObjectType() throws Exception {
    CreateFactTypeRequest request = createRequest()
            .setRelevantObjectBindings(ListUtils.list(new FactObjectBindingDefinition()));
    expectInvalidArgumentException(() -> FactTypeCreateDelegate.create().handle(request), "invalid.object.binding.definition");
  }

  @Test
  public void testCreateFactTypeObjectTypeNotFound() throws Exception {
    CreateFactTypeRequest request = createRequest();
    expectInvalidArgumentException(() -> FactTypeCreateDelegate.create().handle(request), "object.type.not.exist");
  }

  @Test
  public void testCreateFactTypeValidatorNotFound() throws Exception {
    CreateFactTypeRequest request = createRequest();
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    when(getValidatorFactory().get(request.getValidator(), request.getValidatorParameter())).thenThrow(IllegalArgumentException.class);
    expectInvalidArgumentException(() -> FactTypeCreateDelegate.create().handle(request), "validator.not.exist");
  }

  @Test
  public void testCreateFactType() throws Exception {
    CreateFactTypeRequest request = createRequest();
    FactTypeEntity newEntity = new FactTypeEntity();
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    when(getFactManager().saveFactType(any())).thenReturn(newEntity);

    FactTypeCreateDelegate.create().handle(request);
    verify(getFactTypeConverter()).apply(newEntity);
    verify(getFactManager()).saveFactType(argThat(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertEquals(request.getName(), entity.getName());
      assertEquals(request.getValidator(), entity.getValidator());
      assertEquals(request.getValidatorParameter(), entity.getValidatorParameter());

      assertNotNull(entity.getRelevantObjectBindings());
      assertEquals(request.getRelevantObjectBindings().size(), entity.getRelevantObjectBindings().size());
      for (int i = 0; i < request.getRelevantObjectBindings().size(); i++) {
        FactObjectBindingDefinition requestBinding = request.getRelevantObjectBindings().get(i);
        FactTypeEntity.FactObjectBindingDefinition entityBinding = getBindingForSourceObjectTypeID(
                entity.getRelevantObjectBindings(), requestBinding.getSourceObjectType());
        assertEquals(requestBinding.getSourceObjectType(), entityBinding.getSourceObjectTypeID());
        assertEquals(requestBinding.getDestinationObjectType(), entityBinding.getDestinationObjectTypeID());
        assertEquals(requestBinding.isBidirectionalBinding(), entityBinding.isBidirectionalBinding());
      }
      return true;
    }));
  }

  private FactTypeEntity.FactObjectBindingDefinition getBindingForSourceObjectTypeID(
          Set<FactTypeEntity.FactObjectBindingDefinition> bindings, UUID sourceObjectTypeID) {
    return bindings.stream()
            .filter(b -> Objects.equals(b.getSourceObjectTypeID(), sourceObjectTypeID))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
  }

  private CreateFactTypeRequest createRequest() {
    FactObjectBindingDefinition binding1 = new FactObjectBindingDefinition()
            .setSourceObjectType(UUID.randomUUID());
    FactObjectBindingDefinition binding2 = new FactObjectBindingDefinition()
            .setDestinationObjectType(UUID.randomUUID());
    FactObjectBindingDefinition binding3 = new FactObjectBindingDefinition()
            .setSourceObjectType(UUID.randomUUID())
            .setDestinationObjectType(UUID.randomUUID())
            .setBidirectionalBinding(true);

    return new CreateFactTypeRequest()
            .setName("FactType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .setRelevantObjectBindings(ListUtils.list(binding1, binding2, binding3));
  }

  private void expectInvalidArgumentException(InvalidArgumentExceptionTest test, String... messageTemplate) throws Exception {
    try {
      test.execute();
      fail();
    } catch (InvalidArgumentException ex) {
      assertEquals(SetUtils.set(messageTemplate), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
    }
  }

  private interface InvalidArgumentExceptionTest {
    void execute() throws Exception;
  }

}
