package no.mnemonic.act.platform.api.service.v1;

import no.mnemonic.act.platform.api.model.v1.Organization;

import java.util.UUID;

/**
 * Interface defining a function to resolve an Organization by its UUID.
 */
public interface OrganizationResolver {

  /**
   * Resolves an Organization by its UUID.
   *
   * @param id Organization's unique ID
   * @return Resolved Organization
   */
  Organization resolveOrganization(UUID id);

}
