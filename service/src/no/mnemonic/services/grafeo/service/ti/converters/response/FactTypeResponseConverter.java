package no.mnemonic.services.grafeo.service.ti.converters.response;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.NamespaceByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.ObjectTypeByIdResponseResolver;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactTypeResponseConverter implements Function<FactTypeEntity, FactType> {

  private final NamespaceByIdResponseResolver namespaceConverter;
  private final ObjectTypeByIdResponseResolver objectTypeConverter;
  private final FactManager factManager;

  @Inject
  public FactTypeResponseConverter(NamespaceByIdResponseResolver namespaceConverter,
                                   ObjectTypeByIdResponseResolver objectTypeConverter,
                                   FactManager factManager) {
    this.namespaceConverter = namespaceConverter;
    this.objectTypeConverter = objectTypeConverter;
    this.factManager = factManager;
  }

  @Override
  public FactType apply(FactTypeEntity entity) {
    return convertFactType(entity, false);
  }

  private FactType convertFactType(FactTypeEntity entity, boolean skipBindings) {
    if (entity == null) return null;
    return FactType.builder()
            .setId(entity.getId())
            .setNamespace(namespaceConverter.apply(entity.getNamespaceID()))
            .setName(entity.getName())
            .setDefaultConfidence(entity.getDefaultConfidence())
            .setValidator(entity.getValidator())
            .setValidatorParameter(entity.getValidatorParameter())
            .setRelevantObjectBindings(!skipBindings ? convertObjectBindings(entity.getRelevantObjectBindings()) : null)
            .setRelevantFactBindings(!skipBindings ? convertFactBindings(entity.getRelevantFactBindings()) : null)
            .build();
  }

  private List<FactType.FactObjectBindingDefinition> convertObjectBindings(Set<FactTypeEntity.FactObjectBindingDefinition> bindings) {
    if (bindings == null) return null;
    return bindings.stream()
            .map(e -> new FactType.FactObjectBindingDefinition(
                    ObjectUtils.ifNotNull(e.getSourceObjectTypeID(), id -> objectTypeConverter.apply(id).toInfo()),
                    ObjectUtils.ifNotNull(e.getDestinationObjectTypeID(), id -> objectTypeConverter.apply(id).toInfo()),
                    e.isBidirectionalBinding()))
            .collect(Collectors.toList());
  }

  private List<FactType.MetaFactBindingDefinition> convertFactBindings(Set<FactTypeEntity.MetaFactBindingDefinition> bindings) {
    if (bindings == null) return null;
    return bindings.stream()
            .map(e -> new FactType.MetaFactBindingDefinition(ObjectUtils.ifNotNull(e.getFactTypeID(), this::convertFactTypeInfo)))
            .collect(Collectors.toList());
  }

  private FactType.Info convertFactTypeInfo(UUID factTypeID) {
    FactTypeEntity entity = factManager.getFactType(factTypeID);
    // Skip converting bindings in order to avoid infinite recursive resolving of FactTypes.
    return ObjectUtils.ifNotNull(convertFactType(entity, true), FactType::toInfo);
  }
}
