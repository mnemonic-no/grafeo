package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class FactTypeGetByIdDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewTypes);
    FactTypeGetByIdDelegate.create().handle(new GetFactTypeByIdRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchFactTypeNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    FactTypeGetByIdDelegate.create().handle(new GetFactTypeByIdRequest().setId(id));
    verify(getFactManager()).getFactType(id);
  }

  @Test
  public void testFetchFactType() throws Exception {
    UUID id = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();

    when(getFactManager().getFactType(id)).thenReturn(entity);
    FactTypeGetByIdDelegate.create().handle(new GetFactTypeByIdRequest().setId(id));
    verify(getFactTypeConverter()).apply(entity);
  }

}
