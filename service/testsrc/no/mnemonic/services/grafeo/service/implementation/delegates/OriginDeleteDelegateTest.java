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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OriginDeleteDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginResponseConverter originResponseConverter;
  @InjectMocks
  private OriginDeleteDelegate delegate;

  @Test
  public void testDeleteOriginNotExistingOrigin() {
    assertThrows(ObjectNotFoundException.class, () -> delegate.handle(new DeleteOriginRequest().setId(UUID.randomUUID())));
  }

  @Test
  public void testDeleteOriginNoReadPermission() throws Exception {
    OriginEntity entity = new OriginEntity().setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(entity);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new DeleteOriginRequest().setId(entity.getId())));
  }

  @Test
  public void testDeleteOriginNoDeletePermissionWithoutOrganization() throws Exception {
    OriginEntity entity = new OriginEntity().setId(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.deleteGrafeoOrigin);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new DeleteOriginRequest().setId(entity.getId())));
  }

  @Test
  public void testDeleteOriginNoDeletePermissionWithOrganization() throws Exception {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID());
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.deleteGrafeoOrigin, entity.getOrganizationID());
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new DeleteOriginRequest().setId(entity.getId())));
  }

  @Test
  public void testDeleteOriginAlreadyDeleted() {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .addFlag(OriginEntity.Flag.Deleted);
    when(originResolver.apply(entity.getId())).thenReturn(entity);
    assertThrows(InvalidArgumentException.class, () -> delegate.handle(new DeleteOriginRequest().setId(entity.getId())));
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
