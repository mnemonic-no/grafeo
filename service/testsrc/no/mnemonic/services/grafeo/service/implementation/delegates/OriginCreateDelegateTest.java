package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.CreateOriginRequest;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OriginCreateDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResponseConverter originResponseConverter;
  @InjectMocks
  private OriginCreateDelegate delegate;

  @Test
  public void testCreateOriginWithoutGeneralAddPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.addGrafeoOrigin);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new CreateOriginRequest()));
  }

  @Test
  public void testCreateOriginWithoutSpecificAddPermission() throws Exception {
    CreateOriginRequest request = new CreateOriginRequest().setOrganization(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(Collections.singleton(request.getOrganization()));
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.addGrafeoOrigin, request.getOrganization());
    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateOriginWithNonExistingOrganization() {
    CreateOriginRequest request = new CreateOriginRequest().setOrganization(UUID.randomUUID());
    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateOriginWithSameNameExists() {
    CreateOriginRequest request = new CreateOriginRequest().setName("name");
    when(originManager.getOrigin(request.getName())).thenReturn(new OriginEntity());
    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateOriginWithAllFields() throws Exception {
    CreateOriginRequest request = new CreateOriginRequest()
            .setOrganization(UUID.randomUUID())
            .setName("name")
            .setDescription("description")
            .setTrust(0.1f);
    when(securityContext.getAvailableOrganizationID()).thenReturn(Collections.singleton(request.getOrganization()));
    when(originManager.saveOrigin(notNull())).thenReturn(new OriginEntity());

    delegate.handle(request);
    verify(securityContext).checkPermission(FunctionConstants.addGrafeoOrigin, request.getOrganization());
    verify(originResponseConverter).apply(notNull());
    verify(originManager).saveOrigin(argThat(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertEquals(request.getOrganization(), entity.getOrganizationID());
      assertEquals(request.getName(), entity.getName());
      assertEquals(request.getDescription(), entity.getDescription());
      assertEquals(request.getTrust(), entity.getTrust(), 0.0);
      assertEquals(OriginEntity.Type.Group, entity.getType());
      return true;
    }));
  }

  @Test
  public void testCreateOriginWithDefaultFields() throws Exception {
    CreateOriginRequest request = new CreateOriginRequest()
            .setName("name");
    when(originManager.saveOrigin(notNull())).thenReturn(new OriginEntity());

    delegate.handle(request);
    verify(securityContext).checkPermission(FunctionConstants.addGrafeoOrigin);
    verify(originResponseConverter).apply(notNull());
    verify(originManager).saveOrigin(argThat(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertNull(entity.getOrganizationID());
      assertNull(entity.getDescription());
      assertEquals(request.getName(), entity.getName());
      assertEquals(0.8f, entity.getTrust(), 0.0);
      assertEquals(OriginEntity.Type.Group, entity.getType());
      return true;
    }));
  }
}
