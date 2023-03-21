package no.mnemonic.services.grafeo.rest.api.auth;

import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.service.v1.RequestHeader;

/**
 * Interface to resolve {@link Credentials} which are sent to the service back-end inside a {@link RequestHeader}.
 * The {@link Credentials} are used to identify the user and verify the user's session.
 * <p>
 * A {@link CredentialsResolver} implementation must be used in conjunction with a corresponding AccessController
 * implementation in the service back-end which can handle the provided {@link Credentials}.
 */
public interface CredentialsResolver {

  /**
   * Resolve {@link Credentials} identifying the user.
   *
   * @return User identifying {@link Credentials}
   */
  Credentials getCredentials();

  /**
   * Create a {@link RequestHeader} containing the resolved {@link Credentials} (see {@link #getCredentials()}).
   * <p>
   * The returned {@link RequestHeader} can be send directly to the service back-end.
   *
   * @return {@link RequestHeader} with resolved {@link Credentials}
   */
  default RequestHeader getRequestHeader() {
    return RequestHeader.builder()
            .setCredentials(getCredentials())
            .build();
  }
}
