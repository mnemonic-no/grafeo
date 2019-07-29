package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class ObjectTypeGetByIdDelegateTest extends AbstractDelegateTest {

  private ObjectTypeGetByIdDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new ObjectTypeGetByIdDelegate(getSecurityContext(), getObjectTypeConverter());
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewTypes);
    delegate.handle(new GetObjectTypeByIdRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchObjectTypeNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    delegate.handle(new GetObjectTypeByIdRequest().setId(id));
    verify(getObjectManager()).getObjectType(id);
  }

  @Test
  public void testFetchObjectType() throws Exception {
    UUID id = UUID.randomUUID();
    ObjectTypeEntity entity = new ObjectTypeEntity();

    when(getObjectManager().getObjectType(id)).thenReturn(entity);
    delegate.handle(new GetObjectTypeByIdRequest().setId(id));
    verify(getObjectTypeConverter()).apply(entity);
  }
}
