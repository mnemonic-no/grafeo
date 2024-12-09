package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactTypeGetByIdDelegateTest {

  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private FactTypeResponseConverter factTypeResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private FactTypeGetByIdDelegate delegate;

  @Test
  public void testFetchFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoType);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new GetFactTypeByIdRequest()));
  }

  @Test
  public void testFetchFactType() throws Exception {
    UUID id = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();

    when(factTypeRequestResolver.fetchExistingFactType(id)).thenReturn(entity);
    delegate.handle(new GetFactTypeByIdRequest().setId(id));
    verify(factTypeResponseConverter).apply(entity);
  }
}
