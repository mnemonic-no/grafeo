package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.request.v1.DeleteOriginRequest;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
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

public class OriginDeleteDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginResponseConverter originResponseConverter;

  private OriginDeleteDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);
    delegate = new OriginDeleteDelegate(securityContext, originManager, originResolver, originResponseConverter);
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
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.deleteThreatIntelOrigin);
    delegate.handle(new DeleteOriginRequest().setId(entity.getId()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testDeleteOriginNoDeletePermissionWithOrganization() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.deleteThreatIntelOrigin, entity.getOrganizationID());
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
    verify(originManager).saveOrigin(argThat(origin -> origin.isSet(OriginEntity.Flag.Deleted)));
    verify(originResponseConverter).apply(entity);
  }
}
