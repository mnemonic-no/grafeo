package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.request.v1.GetFactByIdRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactGetByIdDelegateTest {

  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private FactResponseConverter factResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;

  private FactGetByIdDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactGetByIdDelegate(securityContext, factRequestResolver, factResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewThreatIntelFact);
    delegate.handle(new GetFactByIdRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactNoAccess() throws Exception {
    UUID id = UUID.randomUUID();
    FactRecord record = new FactRecord();

    when(factRequestResolver.resolveFact(id)).thenReturn(record);
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(record);

    delegate.handle(new GetFactByIdRequest().setId(id));
  }

  @Test
  public void testFetchFact() throws Exception {
    UUID id = UUID.randomUUID();
    FactRecord record = new FactRecord();

    when(factRequestResolver.resolveFact(id)).thenReturn(record);

    delegate.handle(new GetFactByIdRequest().setId(id));
    verify(securityContext).checkReadPermission(record);
    verify(factResponseConverter).apply(record);
  }
}
