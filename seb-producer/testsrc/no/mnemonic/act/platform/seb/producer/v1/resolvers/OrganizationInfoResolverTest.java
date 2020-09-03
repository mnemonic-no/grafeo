package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.seb.model.v1.OrganizationInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OrganizationInfoResolverTest {

  @Mock
  private OrganizationResolver organizationResolver;

  private OrganizationInfoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new OrganizationInfoResolver(organizationResolver);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoOrganizationFound() {
    UUID id = UUID.randomUUID();
    assertNull(resolver.apply(id));
    verify(organizationResolver).resolveOrganization(id);
  }

  @Test
  public void testResolveOrganizationFound() {
    Organization model = Organization.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    when(organizationResolver.resolveOrganization(isA(UUID.class))).thenReturn(model);

    OrganizationInfoSEB seb = resolver.apply(model.getId());
    assertNotNull(seb);
    assertEquals(model.getId(), seb.getId());
    assertEquals(model.getName(), seb.getName());

    verify(organizationResolver).resolveOrganization(model.getId());
  }
}
