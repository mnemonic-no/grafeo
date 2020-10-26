package no.mnemonic.act.platform.auth.properties;

import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.auth.properties.model.SubjectCredentials;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Test;

import static org.junit.Assert.*;

public class PropertiesBasedServiceAccountCredentialsResolverTest {

  private final ServiceAccountSPI resolver = new PropertiesBasedServiceAccountCredentialsResolver(1);

  @Test
  public void testResolveSubjectCredentials() {
    Credentials credentials = resolver.get();
    assertTrue(credentials instanceof SubjectCredentials);
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
