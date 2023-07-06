package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.request.v1.GetOriginByIdRequest;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.OriginResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginGetByIdDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginResponseConverter originResponseConverter;

  private OriginGetByIdDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);
    delegate = new OriginGetByIdDelegate(securityContext, originResolver, originResponseConverter);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchOriginNotFound() throws Exception {
    delegate.handle(new GetOriginByIdRequest().setId(UUID.randomUUID()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchOriginWithoutGeneralViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoOrigin);
    delegate.handle(new GetOriginByIdRequest().setId(UUID.randomUUID()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchOriginWithoutSpecificViewPermission() throws Exception {
    OriginEntity origin = new OriginEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID());
    when(originResolver.apply(origin.getId())).thenReturn(origin);
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(origin);

    delegate.handle(new GetOriginByIdRequest().setId(origin.getId()));
  }

  @Test
  public void testFetchOrigin() throws Exception {
    OriginEntity origin = new OriginEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID());
    when(originResolver.apply(origin.getId())).thenReturn(origin);

    delegate.handle(new GetOriginByIdRequest().setId(origin.getId()));

    verify(originResolver).apply(origin.getId());
    verify(originResponseConverter).apply(origin);
    verify(securityContext).checkPermission(FunctionConstants.viewGrafeoOrigin);
    verify(securityContext).checkReadPermission(origin);
  }
}
