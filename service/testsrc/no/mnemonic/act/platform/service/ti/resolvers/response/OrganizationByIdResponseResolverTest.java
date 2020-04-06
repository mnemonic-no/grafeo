package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationResolver;
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
  private OrganizationResolver organizationResolver;

  private Map<UUID, Organization> responseCache;
  private OrganizationByIdResponseResolver converter;

  @Before
  public void setup() {
    initMocks(this);
    responseCache = new HashMap<>();
    converter = new OrganizationByIdResponseResolver(organizationResolver, responseCache);
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
  public void testConvertUncachedOrganization() {
    UUID id = UUID.randomUUID();
    Organization model = Organization.builder().build();

    when(organizationResolver.resolveOrganization(id)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(organizationResolver).resolveOrganization(id);
  }

  @Test
  public void testConvertUncachedOrganizationNotAvailable() {
    UUID id = UUID.randomUUID();
    Organization model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(organizationResolver).resolveOrganization(id);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
