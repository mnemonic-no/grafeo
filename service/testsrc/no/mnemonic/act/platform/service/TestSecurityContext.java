package no.mnemonic.act.platform.service;

import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;

import static org.mockito.Mockito.mock;

public class TestSecurityContext extends SecurityContext {

  public TestSecurityContext() {
    super(mock(AccessController.class), mock(IdentityResolver.class), mock(Credentials.class));
  }

  public TestSecurityContext(AccessController accessController, IdentityResolver identityResolver, Credentials credentials) {
    super(accessController, identityResolver, credentials);
  }

}
