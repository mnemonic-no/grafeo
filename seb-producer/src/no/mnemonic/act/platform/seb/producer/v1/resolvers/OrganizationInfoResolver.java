package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationSPI;
import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.seb.model.v1.OrganizationInfoSEB;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.services.common.auth.InvalidCredentialsException;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OrganizationInfoResolver implements Function<UUID, OrganizationInfoSEB> {

  private static final Logger LOGGER = Logging.getLogger(OrganizationInfoResolver.class);

  private final OrganizationSPI organizationResolver;
  private final ServiceAccountSPI credentialsResolver;

  @Inject
  public OrganizationInfoResolver(OrganizationSPI organizationResolver, ServiceAccountSPI credentialsResolver) {
    this.organizationResolver = organizationResolver;
    this.credentialsResolver = credentialsResolver;
  }

  @Override
  public OrganizationInfoSEB apply(UUID id) {
    if (id == null) return null;

    Organization organization = resolveOrganization(id);
    if (organization == null) return null;

    return OrganizationInfoSEB.builder()
            .setId(organization.getId())
            .setName(organization.getName())
            .build();
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
