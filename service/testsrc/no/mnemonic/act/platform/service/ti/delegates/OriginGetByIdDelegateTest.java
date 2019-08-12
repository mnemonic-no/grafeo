package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.GetOriginByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginGetByIdDelegateTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private Function<UUID, OriginEntity> originResolver;
  @Mock
  private Function<OriginEntity, Origin> originConverter;

  private OriginGetByIdDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);
    delegate = new OriginGetByIdDelegate(securityContext, originResolver, originConverter);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchOriginNotFound() throws Exception {
    delegate.handle(new GetOriginByIdRequest().setId(UUID.randomUUID()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchOriginWithoutGeneralViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewOrigins);
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
    verify(originConverter).apply(origin);
    verify(securityContext).checkPermission(TiFunctionConstants.viewOrigins);
    verify(securityContext).checkReadPermission(origin);
  }
}
