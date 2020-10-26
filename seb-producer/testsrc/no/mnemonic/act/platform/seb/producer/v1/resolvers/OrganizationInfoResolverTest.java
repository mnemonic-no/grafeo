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

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OrganizationInfoResolverTest {

  @Mock
  private OrganizationSPI organizationResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private OrganizationInfoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    when(credentialsResolver.get()).thenReturn(new Credentials() {});
    resolver = new OrganizationInfoResolver(organizationResolver, credentialsResolver);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveWithInvalidCredentials() throws Exception {
    when(organizationResolver.resolveOrganization(notNull(), isA(UUID.class))).thenThrow(InvalidCredentialsException.class);
    assertNull(resolver.apply(UUID.randomUUID()));
    verify(organizationResolver).resolveOrganization(notNull(), isA(UUID.class));
  }

  @Test
  public void testResolveNoOrganizationFound() throws Exception {
    UUID id = UUID.randomUUID();
    assertNull(resolver.apply(id));
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
}
