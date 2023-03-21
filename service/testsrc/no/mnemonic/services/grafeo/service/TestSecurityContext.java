package no.mnemonic.services.grafeo.service;

import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.auth.IdentitySPI;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;

import static org.mockito.Mockito.mock;

public class TestSecurityContext extends SecurityContext {

  public TestSecurityContext() {
    super(mock(AccessController.class), mock(IdentitySPI.class), mock(Credentials.class));
  }

  public TestSecurityContext(AccessController accessController, IdentitySPI identityResolver, Credentials credentials) {
    super(accessController, identityResolver, credentials);
  }

}
