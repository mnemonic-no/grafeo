package no.mnemonic.services.grafeo.auth.properties;

import no.mnemonic.services.common.auth.model.FunctionIdentity;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SessionDescriptor;
import no.mnemonic.services.common.auth.model.SubjectIdentity;
import no.mnemonic.services.grafeo.auth.IdentitySPI;
import no.mnemonic.services.grafeo.auth.properties.model.FunctionIdentifier;
import no.mnemonic.services.grafeo.auth.properties.model.OrganizationIdentifier;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectDescriptor;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectIdentifier;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PropertiesBasedIdentityResolverTest {

  private final IdentitySPI resolver = new PropertiesBasedIdentityResolver();

  @Test
  public void testResolveFunctionIdentity() {
    String name = "test";
    FunctionIdentity identity = resolver.resolveFunctionIdentity(name);
    assertTrue(identity instanceof FunctionIdentifier);
    assertEquals(name, FunctionIdentifier.class.cast(identity).getName());
  }

  @Test
  public void testResolveOrganizationIdentity() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    OrganizationIdentity identity = resolver.resolveOrganizationIdentity(id);
    assertTrue(identity instanceof OrganizationIdentifier);
    assertEquals(id, OrganizationIdentifier.class.cast(identity).getGlobalID());
  }

  @Test
  public void testResolveOrganizationUUID() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    OrganizationIdentity identity = OrganizationIdentifier.builder().setGlobalID(id).build();
    assertEquals(id, resolver.resolveOrganizationUUID(identity));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testResolveOrganizationUuidWithWrongIdentityType() {
    resolver.resolveOrganizationUUID(new OrganizationIdentity() {});
  }

  @Test
  public void testResolveSubjectIdentity() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    SubjectIdentity identity = resolver.resolveSubjectIdentity(id);
    assertTrue(identity instanceof SubjectIdentifier);
    assertEquals(id, SubjectIdentifier.class.cast(identity).getGlobalID());
  }

  @Test
  public void testResolveSubjectUUID() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    SubjectIdentity identity = SubjectIdentifier.builder().setGlobalID(id).build();
    assertEquals(id, resolver.resolveSubjectUUID(identity));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testResolveSubjectUuidWithWrongIdentityType() {
    resolver.resolveSubjectUUID(new SubjectIdentity() {});
  }

  @Test
  public void testResolveSubjectUuidFromSessionDescriptor() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    SubjectIdentifier identifier = SubjectIdentifier.builder().setGlobalID(id).build();
    SessionDescriptor descriptor = SubjectDescriptor.builder().setIdentifier(identifier).build();
    assertEquals(id, resolver.resolveSubjectUUID(descriptor));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testResolveSubjectUuidFromSessionDescriptorWithWrongDescriptorType() {
    resolver.resolveSubjectUUID(new SessionDescriptor() {});
  }
}
