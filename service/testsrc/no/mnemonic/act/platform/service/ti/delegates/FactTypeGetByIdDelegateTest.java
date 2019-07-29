package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class FactTypeGetByIdDelegateTest extends AbstractDelegateTest {

  private FactTypeGetByIdDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactTypeGetByIdDelegate(getSecurityContext(), getFactTypeConverter());
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewTypes);
    delegate.handle(new GetFactTypeByIdRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchFactTypeNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    delegate.handle(new GetFactTypeByIdRequest().setId(id));
    verify(getFactManager()).getFactType(id);
  }

  @Test
  public void testFetchFactType() throws Exception {
    UUID id = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();

    when(getFactManager().getFactType(id)).thenReturn(entity);
    delegate.handle(new GetFactTypeByIdRequest().setId(id));
    verify(getFactTypeConverter()).apply(entity);
  }
}
