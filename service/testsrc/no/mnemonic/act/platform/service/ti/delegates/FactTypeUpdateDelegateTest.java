package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FactTypeUpdateDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testUpdateFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.updateTypes);
    FactTypeUpdateDelegate.create().handle(createRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testUpdateFactTypeNotExisting() throws Exception {
    UpdateFactTypeRequest request = createRequest();
    FactTypeUpdateDelegate.create().handle(request);
  }

  @Test
  public void testUpdateFactTypeWithExistingName() throws Exception {
    UpdateFactTypeRequest request = createRequest().setAddObjectBindings(null);
    when(getFactManager().getFactType(request.getId())).thenReturn(new FactTypeEntity());
    when(getFactManager().getFactType(request.getName())).thenReturn(new FactTypeEntity());
    expectInvalidArgumentException(() -> FactTypeUpdateDelegate.create().handle(request), "fact.type.exist");
  }

  @Test
  public void testUpdateFactTypeWithoutSpecifiedObjectType() throws Exception {
    UpdateFactTypeRequest request = createRequest().setAddObjectBindings(ListUtils.list(new FactObjectBindingDefinition()));
    when(getFactManager().getFactType(request.getId())).thenReturn(new FactTypeEntity());
    expectInvalidArgumentException(() -> FactTypeUpdateDelegate.create().handle(request), "invalid.object.binding.definition");
  }

  @Test
  public void testUpdateFactTypeWithNonExistingObjectType() throws Exception {
    UpdateFactTypeRequest request = createRequest().setName(null);
    when(getFactManager().getFactType(request.getId())).thenReturn(new FactTypeEntity());
    expectInvalidArgumentException(() -> FactTypeUpdateDelegate.create().handle(request), "object.type.not.exist");
  }

  @Test
  public void testUpdateFactType() throws Exception {
    UpdateFactTypeRequest request = createRequest();
    FactTypeEntity entity = new FactTypeEntity();
    when(getFactManager().getFactType(request.getId())).thenReturn(entity);
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    when(getFactManager().saveFactType(argThat(e -> {
      assertSame(entity, e);
      assertEquals(request.getName(), e.getName());

      assertNotNull(entity.getRelevantObjectBindings());
      assertEquals(request.getAddObjectBindings().size(), entity.getRelevantObjectBindings().size());
      for (int i = 0; i < request.getAddObjectBindings().size(); i++) {
        FactObjectBindingDefinition requestBinding = request.getAddObjectBindings().get(i);
        FactTypeEntity.FactObjectBindingDefinition entityBinding = entity.getRelevantObjectBindings().get(i);
        assertEquals(requestBinding.getSourceObjectType(), entityBinding.getSourceObjectTypeID());
        assertEquals(requestBinding.getDestinationObjectType(), entityBinding.getDestinationObjectTypeID());
        assertEquals(requestBinding.isBidirectionalBinding(), entityBinding.isBidirectionalBinding());
      }
      return true;
    }))).thenReturn(entity);

    FactTypeUpdateDelegate.create().handle(request);
    verify(getFactTypeConverter()).apply(entity);
  }

  private UpdateFactTypeRequest createRequest() {
    FactObjectBindingDefinition binding1 = new FactObjectBindingDefinition()
            .setSourceObjectType(UUID.randomUUID());
    FactObjectBindingDefinition binding2 = new FactObjectBindingDefinition()
            .setDestinationObjectType(UUID.randomUUID());
    FactObjectBindingDefinition binding3 = new FactObjectBindingDefinition()
            .setSourceObjectType(UUID.randomUUID())
            .setDestinationObjectType(UUID.randomUUID())
            .setBidirectionalBinding(true);

    return new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName("newName")
            .addAddObjectBinding(binding1)
            .addAddObjectBinding(binding2)
            .addAddObjectBinding(binding3);
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
