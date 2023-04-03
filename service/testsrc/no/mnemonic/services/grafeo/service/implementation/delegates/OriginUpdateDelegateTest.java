package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.request.v1.UpdateOriginRequest;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.OriginResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginUpdateDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginResponseConverter originResponseConverter;

  private OriginUpdateDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);
    delegate = new OriginUpdateDelegate(securityContext, originManager, originResolver, originResponseConverter);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testUpdateOriginNotExistingOrigin() throws Exception {
    delegate.handle(new UpdateOriginRequest().setId(UUID.randomUUID()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testUpdateOriginNoReadPermission() throws Exception {
    OriginEntity entity = new OriginEntity().setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(entity);
    delegate.handle(new UpdateOriginRequest().setId(entity.getId()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testUpdateOriginNoUpdatePermissionWithoutOrganization() throws Exception {
    OriginEntity entity = new OriginEntity().setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.updateThreatIntelOrigin);
    delegate.handle(new UpdateOriginRequest().setId(entity.getId()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testUpdateOriginNoUpdatePermissionWithOrganization() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.updateThreatIntelOrigin, entity.getOrganizationID());
    delegate.handle(new UpdateOriginRequest().setId(entity.getId()));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateOriginDeleted() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .addFlag(OriginEntity.Flag.Deleted);
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    delegate.handle(new UpdateOriginRequest().setId(entity.getId()));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateOriginOrganizationNotExisting() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    delegate.handle(new UpdateOriginRequest()
            .setId(entity.getId())
            .setOrganization(UUID.randomUUID())
    );
  }

  @Test
  public void testUpdateOriginOrganization() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID());
    UpdateOriginRequest request = new UpdateOriginRequest()
            .setId(entity.getId())
            .setOrganization(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    when(securityContext.getAvailableOrganizationID()).thenReturn(Collections.singleton(request.getOrganization()));

    delegate.handle(request);
    verify(securityContext).checkPermission(FunctionConstants.updateThreatIntelOrigin, request.getOrganization());
    verify(originManager).saveOrigin(argThat(origin -> Objects.equals(request.getOrganization(), origin.getOrganizationID())));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateOriginNameExisting() throws Exception {
    OriginEntity existingOrigin = new OriginEntity()
            .setId(UUID.randomUUID());
    OriginEntity otherOrigin = new OriginEntity()
            .setId(UUID.randomUUID())
            .setName("otherOrigin");
    when(originResolver.apply(existingOrigin.getId())).thenReturn(existingOrigin);
    when(originManager.getOrigin(otherOrigin.getName())).thenReturn(otherOrigin);
    delegate.handle(new UpdateOriginRequest()
            .setId(existingOrigin.getId())
            .setName(otherOrigin.getName())
    );
  }

  @Test
  public void testUpdateOriginName() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID());
    UpdateOriginRequest request = new UpdateOriginRequest()
            .setId(entity.getId())
            .setName("updated");
    when(originResolver.apply(entity.getId())).thenReturn(entity);

    delegate.handle(request);
    verify(originManager).getOrigin(request.getName());
    verify(originManager).saveOrigin(argThat(origin -> Objects.equals(request.getName(), origin.getName())));
  }

  @Test
  public void testUpdateOriginDescription() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID());
    UpdateOriginRequest request = new UpdateOriginRequest()
            .setId(entity.getId())
            .setDescription("updated");
    when(originResolver.apply(entity.getId())).thenReturn(entity);

    delegate.handle(request);
    verify(originManager).saveOrigin(argThat(origin -> Objects.equals(request.getDescription(), origin.getDescription())));
  }

  @Test
  public void testUpdateOriginTrust() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID());
    UpdateOriginRequest request = new UpdateOriginRequest()
            .setId(entity.getId())
            .setTrust(0.1f);
    when(originResolver.apply(entity.getId())).thenReturn(entity);

    delegate.handle(request);
    verify(originManager).saveOrigin(argThat(origin -> Objects.equals(request.getTrust(), origin.getTrust())));
  }

  @Test
  public void testUpdateOriginCallsConverter() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    when(originManager.saveOrigin(entity)).thenReturn(entity);

    delegate.handle(new UpdateOriginRequest().setId(entity.getId()));
    verify(originManager).saveOrigin(entity);
    verify(originResponseConverter).apply(entity);
  }
}
