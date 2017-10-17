package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.Direction;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
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
    verify(getFactManager()).getFactType(request.getId());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateFactTypeWithExistingName() throws Exception {
    UpdateFactTypeRequest request = createRequest().setAddObjectBindings(null);
    when(getFactManager().getFactType(request.getId())).thenReturn(new FactTypeEntity());
    when(getFactManager().getFactType(request.getName())).thenReturn(new FactTypeEntity());
    FactTypeUpdateDelegate.create().handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateFactTypeWithNonExistingObjectType() throws Exception {
    UpdateFactTypeRequest request = createRequest().setName(null);
    when(getFactManager().getFactType(request.getId())).thenReturn(new FactTypeEntity());
    FactTypeUpdateDelegate.create().handle(request);
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
        assertEquals(requestBinding.getObjectType(), entityBinding.getObjectTypeID());
        assertEquals(requestBinding.getDirection().name(), entityBinding.getDirection().name());
      }
      return true;
    }))).thenReturn(entity);

    FactTypeUpdateDelegate.create().handle(request);
    verify(getFactTypeConverter()).apply(entity);
  }

  private UpdateFactTypeRequest createRequest() {
    FactObjectBindingDefinition binding1 = new FactObjectBindingDefinition()
            .setObjectType(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactObjectBindingDefinition binding2 = new FactObjectBindingDefinition()
            .setObjectType(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);

    return new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName("newName")
            .addAddObjectBinding(binding1)
            .addAddObjectBinding(binding2);
  }

}
