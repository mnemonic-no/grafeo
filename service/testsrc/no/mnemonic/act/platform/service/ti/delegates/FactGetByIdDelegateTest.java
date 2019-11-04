package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.converters.FactConverter;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class FactGetByIdDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactResolver factResolver;
  @Mock
  private FactConverter factConverter;

  private FactGetByIdDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactGetByIdDelegate(getSecurityContext(), factResolver, factConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    delegate.handle(new GetFactByIdRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactNoAccess() throws Exception {
    UUID id = UUID.randomUUID();
    FactRecord record = new FactRecord();

    when(factResolver.resolveFact(id)).thenReturn(record);
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(record);

    delegate.handle(new GetFactByIdRequest().setId(id));
  }

  @Test
  public void testFetchFact() throws Exception {
    UUID id = UUID.randomUUID();
    FactRecord record = new FactRecord();

    when(factResolver.resolveFact(id)).thenReturn(record);

    delegate.handle(new GetFactByIdRequest().setId(id));
    verify(getSecurityContext()).checkReadPermission(record);
    verify(factConverter).apply(record);
  }
}
