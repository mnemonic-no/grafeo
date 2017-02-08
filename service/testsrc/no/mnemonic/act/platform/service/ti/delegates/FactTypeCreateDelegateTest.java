package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.Direction;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.commons.testtools.MockitoTools.match;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class FactTypeCreateDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addTypes);
    FactTypeCreateDelegate.create().handle(createRequest());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateAlreadyExistingFactType() throws Exception {
    CreateFactTypeRequest request = createRequest();
    when(getFactManager().getFactType(request.getName())).thenReturn(new FactTypeEntity());
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    FactTypeCreateDelegate.create().handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateFactTypeObjectTypeNotFound() throws Exception {
    CreateFactTypeRequest request = createRequest();
    FactTypeCreateDelegate.create().handle(request);
    verify(getObjectManager(), times(request.getRelevantObjectBindings().size())).getObjectType(isA(UUID.class));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateFactTypeEntityHandlerNotFound() throws Exception {
    CreateFactTypeRequest request = createRequest();
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    when(getEntityHandlerFactory().get(request.getEntityHandler(), request.getEntityHandlerParameter())).thenThrow(IllegalArgumentException.class);
    FactTypeCreateDelegate.create().handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateFactTypeValidatorNotFound() throws Exception {
    CreateFactTypeRequest request = createRequest();
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    when(getValidatorFactory().get(request.getValidator(), request.getValidatorParameter())).thenThrow(IllegalArgumentException.class);
    FactTypeCreateDelegate.create().handle(request);
  }

  @Test
  public void testCreateFactType() throws Exception {
    CreateFactTypeRequest request = createRequest();
    FactTypeEntity newEntity = new FactTypeEntity();
    when(getObjectManager().getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    when(getFactManager().saveFactType(any())).thenReturn(newEntity);

    FactTypeCreateDelegate.create().handle(request);
    verify(getFactTypeConverter()).apply(newEntity);
    verify(getFactManager()).saveFactType(match(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertEquals(request.getName(), entity.getName());
      assertEquals(request.getEntityHandler(), entity.getEntityHandler());
      assertEquals(request.getEntityHandlerParameter(), entity.getEntityHandlerParameter());
      assertEquals(request.getValidator(), entity.getValidator());
      assertEquals(request.getValidatorParameter(), entity.getValidatorParameter());

      assertNotNull(entity.getRelevantObjectBindings());
      assertEquals(request.getRelevantObjectBindings().size(), entity.getRelevantObjectBindings().size());
      for (int i = 0; i < request.getRelevantObjectBindings().size(); i++) {
        FactObjectBindingDefinition requestBinding = request.getRelevantObjectBindings().get(i);
        FactTypeEntity.FactObjectBindingDefinition entityBinding = entity.getRelevantObjectBindings().get(i);
        assertEquals(requestBinding.getObjectType(), entityBinding.getObjectTypeID());
        assertEquals(requestBinding.getDirection().name(), entityBinding.getDirection().name());
      }
      return true;
    }));
  }

  private CreateFactTypeRequest createRequest() {
    FactObjectBindingDefinition binding1 = new FactObjectBindingDefinition()
            .setObjectType(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactObjectBindingDefinition binding2 = new FactObjectBindingDefinition()
            .setObjectType(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);

    return new CreateFactTypeRequest()
            .setName("FactType")
            .setEntityHandler("EntityHandler")
            .setEntityHandlerParameter("EntityHandlerParameter")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .setRelevantObjectBindings(ListUtils.list(binding1, binding2));
  }

}
