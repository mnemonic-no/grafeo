package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.CreateOriginRequest;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginCreateDelegateTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private Function<OriginEntity, Origin> originConverter;

  private OriginCreateDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);
    delegate = new OriginCreateDelegate(securityContext, originManager, originConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateOriginWithoutGeneralAddPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.addOrigins);
    delegate.handle(new CreateOriginRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateOriginWithoutSpecificAddPermission() throws Exception {
    CreateOriginRequest request = new CreateOriginRequest().setOrganization(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(Collections.singleton(request.getOrganization()));
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.addOrigins, request.getOrganization());
    delegate.handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateOriginWithNonExistingOrganization() throws Exception {
    CreateOriginRequest request = new CreateOriginRequest().setOrganization(UUID.randomUUID());
    delegate.handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateOriginWithSameNameExists() throws Exception {
    CreateOriginRequest request = new CreateOriginRequest().setName("name");
    when(originManager.getOrigin(request.getName())).thenReturn(new OriginEntity());
    delegate.handle(request);
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
    verify(securityContext).checkPermission(TiFunctionConstants.addOrigins, request.getOrganization());
    verify(originConverter).apply(notNull());
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
    verify(securityContext).checkPermission(TiFunctionConstants.addOrigins);
    verify(originConverter).apply(notNull());
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
