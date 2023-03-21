package no.mnemonic.services.grafeo.service;

import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;

/**
 * Every Service implementation needs to implement this interface in order to use cross-cutting functionality provided by aspects.
 */
public interface Service {

  /**
   * Create a service-specific SecurityContext.
   *
   * @param credentials Credentials provided in request
   * @return New SecurityContext
   */
  SecurityContext createSecurityContext(Credentials credentials);

}
