package no.mnemonic.services.grafeo.service.ti.resolvers.response;

import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.auth.OrganizationSPI;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OrganizationByIdResponseResolverTest {

  @Mock
  private OrganizationSPI organizationResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private Map<UUID, Organization> responseCache;
  private OrganizationByIdResponseResolver converter;

  @Before
  public void setup() {
    initMocks(this);
    when(credentialsResolver.get()).thenReturn(new Credentials() {});
    responseCache = new HashMap<>();
    converter = new OrganizationByIdResponseResolver(organizationResolver, credentialsResolver, responseCache);
  }

  @Test
  public void testConvertCachedOrganization() {
    UUID id = UUID.randomUUID();
    Organization model = Organization.builder().build();
    responseCache.put(id, model);

    assertSame(model, converter.apply(id));
    verifyNoInteractions(organizationResolver);
  }

  @Test
  public void testConvertUncachedOrganization() throws Exception {
    UUID id = UUID.randomUUID();
    Organization model = Organization.builder().build();

    when(organizationResolver.resolveOrganization(notNull(), isA(UUID.class))).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(organizationResolver).resolveOrganization(notNull(), eq(id));
  }

  @Test
  public void testConvertUncachedOrganizationWithInvalidCredentials() throws Exception {
    UUID id = UUID.randomUUID();
    when(organizationResolver.resolveOrganization(notNull(), isA(UUID.class))).thenThrow(InvalidCredentialsException.class);

    Organization model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(organizationResolver).resolveOrganization(notNull(), eq(id));
  }

  @Test
  public void testConvertUncachedOrganizationNotAvailable() throws Exception {
    UUID id = UUID.randomUUID();
    Organization model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(organizationResolver).resolveOrganization(notNull(), eq(id));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
