package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.auth.OrganizationSPI;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class OrganizationByIdResponseResolver implements Function<UUID, Organization> {

  private static final Logger LOGGER = Logging.getLogger(OrganizationByIdResponseResolver.class);

  private final OrganizationSPI organizationResolver;
  private final ServiceAccountSPI credentialsResolver;
  private final Map<UUID, Organization> responseCache;

  @Inject
  public OrganizationByIdResponseResolver(OrganizationSPI organizationResolver,
                                          ServiceAccountSPI credentialsResolver,
                                          Map<UUID, Organization> responseCache) {
    this.organizationResolver = organizationResolver;
    this.credentialsResolver = credentialsResolver;
    this.responseCache = responseCache;
  }

  @Override
  public Organization apply(UUID id) {
    if (id == null) return null;
    return responseCache.computeIfAbsent(id, this::resolveUncached);
  }

  private Organization resolveUncached(UUID id) {
    return ObjectUtils.ifNull(resolveOrganization(id), Organization.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }

  private Organization resolveOrganization(UUID id) {
    try {
      return organizationResolver.resolveOrganization(credentialsResolver.get(), id);
    } catch (InvalidCredentialsException ex) {
      LOGGER.warning(ex, "Could not resolve Organization for id = %s.", id);
      return null;
    }
  }
}
