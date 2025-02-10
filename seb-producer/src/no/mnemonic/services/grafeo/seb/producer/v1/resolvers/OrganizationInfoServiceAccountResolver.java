package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.auth.OrganizationSPI;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import no.mnemonic.services.grafeo.seb.model.v1.OrganizationInfoSEB;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class OrganizationInfoServiceAccountResolver implements Function<UUID, OrganizationInfoSEB> {

  private static final Logger LOGGER = Logging.getLogger(OrganizationInfoServiceAccountResolver.class);

  private final OrganizationSPI organizationResolver;
  private final ServiceAccountSPI credentialsResolver;
  // This utilizes the same cache as OrganizationByIdResponseResolver as both resolvers are bound to the service account.
  private final Map<UUID, Organization> organizationCache;

  @Inject
  public OrganizationInfoServiceAccountResolver(OrganizationSPI organizationResolver,
                                                ServiceAccountSPI credentialsResolver,
                                                Map<UUID, Organization> organizationCache) {
    this.organizationResolver = organizationResolver;
    this.credentialsResolver = credentialsResolver;
    this.organizationCache = organizationCache;
  }

  @Override
  public OrganizationInfoSEB apply(UUID id) {
    if (id == null) return null;

    Organization organization = organizationCache.computeIfAbsent(id, this::resolveUncached);
    return OrganizationInfoSEB.builder()
            .setId(organization.getId())
            .setName(organization.getName())
            .build();
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
