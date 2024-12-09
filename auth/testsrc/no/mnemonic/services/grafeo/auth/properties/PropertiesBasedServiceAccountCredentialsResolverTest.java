package no.mnemonic.services.grafeo.auth.properties;

import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectCredentials;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PropertiesBasedServiceAccountCredentialsResolverTest {

  private final ServiceAccountSPI resolver = new PropertiesBasedServiceAccountCredentialsResolver(1);

  @Test
  public void testResolveSubjectCredentials() {
    Credentials credentials = resolver.get();
    assertInstanceOf(SubjectCredentials.class, credentials);
    assertEquals(1, ((SubjectCredentials) credentials).getSubjectID());
  }

  @Test
  public void testResolveCredentialsTwice() {
    Credentials first = resolver.get();
    Credentials second = resolver.get();
    assertNotSame(first, second);
    assertEquals(((SubjectCredentials) first).getSubjectID(), ((SubjectCredentials) second).getSubjectID());
  }
}
