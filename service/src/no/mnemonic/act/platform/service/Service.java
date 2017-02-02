package no.mnemonic.act.platform.service;

import no.mnemonic.act.platform.service.contexts.SecurityContext;

/**
 * Every Service implementation needs to implement this interface in order to use cross-cutting functionality provided by aspects.
 */
public interface Service {

  SecurityContext createSecurityContext();

}
