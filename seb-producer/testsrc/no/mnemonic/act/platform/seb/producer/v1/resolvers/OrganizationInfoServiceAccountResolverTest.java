package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationSPI;
import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.seb.model.v1.OrganizationInfoSEB;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OrganizationInfoServiceAccountResolverTest {

  @Mock
  private OrganizationSPI organizationResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private Map<UUID, Organization> organizationCache;
  private OrganizationInfoServiceAccountResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    when(credentialsResolver.get()).thenReturn(new Credentials() {});
    organizationCache = new HashMap<>();
    resolver = new OrganizationInfoServiceAccountResolver(organizationResolver, credentialsResolver, organizationCache);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveWithInvalidCredentials() throws Exception {
    UUID id = UUID.randomUUID();
    when(organizationResolver.resolveOrganization(notNull(), isA(UUID.class))).thenThrow(InvalidCredentialsException.class);

    OrganizationInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());
    assertEquals("N/A", seb.getName());

    verify(organizationResolver).resolveOrganization(notNull(), eq(id));
  }

  @Test
  public void testResolveNoOrganizationFound() throws Exception {
    UUID id = UUID.randomUUID();

    OrganizationInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());
    assertEquals("N/A", seb.getName());

    verify(organizationResolver).resolveOrganization(notNull(), eq(id));
  }

  @Test
  public void testResolveOrganizationFound() throws Exception {
    Organization model = Organization.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    when(organizationResolver.resolveOrganization(notNull(), isA(UUID.class))).thenReturn(model);

    OrganizationInfoSEB seb = resolver.apply(model.getId());
    assertNotNull(seb);
    assertEquals(model.getId(), seb.getId());
    assertEquals(model.getName(), seb.getName());

    verify(organizationResolver).resolveOrganization(notNull(), eq(model.getId()));
  }

  @Test
  public void testResolveCachesOrganization() throws Exception {
    Organization model = Organization.builder().setId(UUID.randomUUID()).build();
    when(organizationResolver.resolveOrganization(notNull(), isA(UUID.class))).thenReturn(model);

    resolver.apply(model.getId());
    assertEquals(1, organizationCache.size());
    assertSame(model, organizationCache.get(model.getId()));
  }

  @Test
  public void testResolvePreviouslyCachedOrganization() {
    Organization model = Organization.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    organizationCache.put(model.getId(), model);

    OrganizationInfoSEB seb = resolver.apply(model.getId());
    assertNotNull(seb);
    assertEquals(model.getId(), seb.getId());
    assertEquals(model.getName(), seb.getName());

    verifyNoInteractions(organizationResolver);
  }
}
