package no.mnemonic.act.platform.auth;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;

import java.util.UUID;

/**
 * Common interface defining functions to resolve Organizations. Note that Organizations here are ACT model objects (see {@link Organization}).
 *
 * This interface can be implemented by an AccessController implementation or by another service able to resolve Organizations.
 * Whatever implementation is chosen an implementation must be provided when using a non-standard AccessController implementation.
 */
public interface OrganizationResolver {

  /**
   * Resolves an Organization by its UUID.
   *
   * @param id Organization's unique ID
   * @return Resolved Organization
   */
  Organization resolveOrganization(UUID id);

  /**
   * Resolves the affiliated Organization for a Subject identified by the given credentials.
   *
   * @param credentials Credentials identifying a Subject
   * @return Affiliated Organization for a Subject
   * @throws InvalidCredentialsException Thrown if the given credentials are invalid.
   */
  Organization resolveCurrentUserAffiliation(Credentials credentials) throws InvalidCredentialsException;

}
