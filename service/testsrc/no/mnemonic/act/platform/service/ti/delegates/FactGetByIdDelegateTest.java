package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class FactGetByIdDelegateTest extends AbstractDelegateTest {

  private FactGetByIdDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactGetByIdDelegate(getSecurityContext(), getFactConverter());
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    delegate.handle(new GetFactByIdRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchFactNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    delegate.handle(new GetFactByIdRequest().setId(id));
    verify(getFactManager()).getFact(id);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactNoAccess() throws Exception {
    UUID id = UUID.randomUUID();
    FactEntity entity = new FactEntity();

    when(getFactManager().getFact(id)).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(entity);

    delegate.handle(new GetFactByIdRequest().setId(id));
  }

  @Test
  public void testFetchFact() throws Exception {
    UUID id = UUID.randomUUID();
    FactEntity entity = new FactEntity();

    when(getFactManager().getFact(id)).thenReturn(entity);
    delegate.handle(new GetFactByIdRequest().setId(id));
    verify(getFactConverter()).apply(entity);
  }
}
