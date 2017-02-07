package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class ObjectTypeGetByIdDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewTypes);
    ObjectTypeGetByIdDelegate.create().handle(new GetObjectTypeByIdRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchObjectTypeNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    ObjectTypeGetByIdDelegate.create().handle(new GetObjectTypeByIdRequest().setId(id));
    verify(getObjectManager()).getObjectType(id);
  }

  @Test
  public void testFetchObjectType() throws Exception {
    UUID id = UUID.randomUUID();
    ObjectTypeEntity entity = new ObjectTypeEntity();

    when(getObjectManager().getObjectType(id)).thenReturn(entity);
    ObjectTypeGetByIdDelegate.create().handle(new GetObjectTypeByIdRequest().setId(id));
    verify(getObjectTypeConverter()).apply(entity);
  }

}
