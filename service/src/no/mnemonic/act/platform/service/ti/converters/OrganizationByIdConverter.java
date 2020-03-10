package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OrganizationByIdConverter implements Function<UUID, Organization> {

  private final OrganizationResolver organizationResolver;

  @Inject
  public OrganizationByIdConverter(OrganizationResolver organizationResolver) {
    this.organizationResolver = organizationResolver;
  }

  @Override
  public Organization apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNull(organizationResolver.resolveOrganization(id), Organization.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
