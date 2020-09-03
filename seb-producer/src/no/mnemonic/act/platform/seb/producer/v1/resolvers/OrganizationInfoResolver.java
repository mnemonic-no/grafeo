package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.seb.model.v1.OrganizationInfoSEB;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OrganizationInfoResolver implements Function<UUID, OrganizationInfoSEB> {

  private final OrganizationResolver organizationResolver;

  @Inject
  public OrganizationInfoResolver(OrganizationResolver organizationResolver) {
    this.organizationResolver = organizationResolver;
  }

  @Override
  public OrganizationInfoSEB apply(UUID id) {
    if (id == null) return null;

    Organization organization = organizationResolver.resolveOrganization(id);
    if (organization == null) return null;

    return OrganizationInfoSEB.builder()
            .setId(organization.getId())
            .setName(organization.getName())
            .build();
  }
}
