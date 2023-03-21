package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.FactTypeResponseConverter;
import no.mnemonic.services.grafeo.service.ti.resolvers.request.FactTypeRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeGetByIdDelegateTest {

  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private FactTypeResponseConverter factTypeResponseConverter;
  @Mock
  private TiSecurityContext securityContext;

  private FactTypeGetByIdDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactTypeGetByIdDelegate(
      securityContext,
      factTypeResponseConverter,
      factTypeRequestResolver);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelType);
    delegate.handle(new GetFactTypeByIdRequest());
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
