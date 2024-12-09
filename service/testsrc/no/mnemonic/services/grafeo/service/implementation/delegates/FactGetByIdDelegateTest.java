package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.request.v1.GetFactByIdRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactGetByIdDelegateTest {

  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private FactResponseConverter factResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private FactGetByIdDelegate delegate;

  @Test
  public void testFetchFactWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoFact);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new GetFactByIdRequest()));
  }

  @Test
  public void testFetchFactNoAccess() throws Exception {
    UUID id = UUID.randomUUID();
    FactRecord record = new FactRecord();

    when(factRequestResolver.resolveFact(id)).thenReturn(record);
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(record);

    assertThrows(AccessDeniedException.class, () -> delegate.handle(new GetFactByIdRequest().setId(id)));
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
