package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.commons.testtools.MockitoTools.match;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class ObjectTypeUpdateDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testUpdateObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.updateTypes);
    ObjectTypeUpdateDelegate.create().handle(createRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testUpdateObjectTypeNotExisting() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    ObjectTypeUpdateDelegate.create().handle(request);
    verify(getObjectManager()).getObjectType(request.getId());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateObjectTypeWithExistingName() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    when(getObjectManager().getObjectType(request.getId())).thenReturn(new ObjectTypeEntity());
    when(getObjectManager().getObjectType(request.getName())).thenReturn(new ObjectTypeEntity());
    ObjectTypeUpdateDelegate.create().handle(request);
  }

  @Test
  public void testUpdateObjectType() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    ObjectTypeEntity entity = new ObjectTypeEntity();
    when(getObjectManager().getObjectType(request.getId())).thenReturn(entity);
    when(getObjectManager().saveObjectType(match(e -> {
      assertSame(entity, e);
      assertEquals(request.getName(), e.getName());
      return true;
    }))).thenReturn(entity);

    ObjectTypeUpdateDelegate.create().handle(request);
    verify(getObjectTypeConverter()).apply(entity);
  }

  private UpdateObjectTypeRequest createRequest() {
    return new UpdateObjectTypeRequest()
            .setId(UUID.randomUUID())
            .setName("newName");
  }

}
