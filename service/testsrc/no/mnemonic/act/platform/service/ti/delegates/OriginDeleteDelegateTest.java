package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.DeleteOriginRequest;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.OriginConverter;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginDeleteDelegateTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginConverter originConverter;

  private OriginDeleteDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);
    delegate = new OriginDeleteDelegate(securityContext, originManager, originResolver, originConverter);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testDeleteOriginNotExistingOrigin() throws Exception {
    delegate.handle(new DeleteOriginRequest().setId(UUID.randomUUID()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testDeleteOriginNoReadPermission() throws Exception {
    OriginEntity entity = new OriginEntity().setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(entity);
    delegate.handle(new DeleteOriginRequest().setId(entity.getId()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testDeleteOriginNoDeletePermissionWithoutOrganization() throws Exception {
    OriginEntity entity = new OriginEntity().setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.deleteOrigins);
    delegate.handle(new DeleteOriginRequest().setId(entity.getId()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testDeleteOriginNoDeletePermissionWithOrganization() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.deleteOrigins, entity.getOrganizationID());
    delegate.handle(new DeleteOriginRequest().setId(entity.getId()));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testDeleteOriginAlreadyDeleted() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .addFlag(OriginEntity.Flag.Deleted);
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    delegate.handle(new DeleteOriginRequest().setId(entity.getId()));
  }

  @Test
  public void testDeleteOriginMarksAsDeleted() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    when(originManager.saveOrigin(entity)).thenReturn(entity);

    delegate.handle(new DeleteOriginRequest().setId(entity.getId()));
    verify(originManager).saveOrigin(argThat(origin -> origin.getFlags().contains(OriginEntity.Flag.Deleted)));
    verify(originConverter).apply(entity);
  }
}
