package no.mnemonic.services.grafeo.service.implementation.converters.response;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.NamespaceByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.OrganizationByIdResponseResolver;

import javax.inject.Inject;
import java.util.function.Function;

public class OriginResponseConverter implements Function<OriginEntity, Origin> {

  private final NamespaceByIdResponseResolver namespaceConverter;
  private final OrganizationByIdResponseResolver organizationConverter;

  @Inject
  public OriginResponseConverter(NamespaceByIdResponseResolver namespaceConverter,
                                 OrganizationByIdResponseResolver organizationConverter) {
    this.namespaceConverter = namespaceConverter;
    this.organizationConverter = organizationConverter;
  }

  @Override
  public Origin apply(OriginEntity entity) {
    if (entity == null) return null;
    return Origin.builder()
            .setId(entity.getId())
            .setNamespace(namespaceConverter.apply(entity.getNamespaceID()))
            .setOrganization(ObjectUtils.ifNotNull(organizationConverter.apply(entity.getOrganizationID()), Organization::toInfo))
            .setName(entity.getName())
            .setDescription(entity.getDescription())
            .setTrust(entity.getTrust())
            .setType(ObjectUtils.ifNotNull(entity.getType(), type -> Origin.Type.valueOf(type.name())))
            .setFlags(SetUtils.set(entity.getFlags(), flag -> Origin.Flag.valueOf(flag.name())))
            .build();
  }
}
