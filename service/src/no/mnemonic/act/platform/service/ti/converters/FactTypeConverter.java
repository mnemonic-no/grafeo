package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactTypeConverter implements Converter<FactTypeEntity, FactType> {

  private final Function<UUID, Namespace> namespaceConverter;
  private final Function<UUID, ObjectType> objectTypeConverter;
  private final Function<UUID, FactTypeEntity> factTypeEntityResolver;

  @Inject
  public FactTypeConverter(Function<UUID, Namespace> namespaceConverter,
                           Function<UUID, ObjectType> objectTypeConverter,
                           Function<UUID, FactTypeEntity> factTypeEntityResolver) {
    this.namespaceConverter = namespaceConverter;
    this.objectTypeConverter = objectTypeConverter;
    this.factTypeEntityResolver = factTypeEntityResolver;
  }

  @Override
  public Class<FactTypeEntity> getSourceType() {
    return FactTypeEntity.class;
  }

  @Override
  public Class<FactType> getTargetType() {
    return FactType.class;
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
    FactTypeEntity entity = factTypeEntityResolver.apply(factTypeID);
    // Skip converting bindings in order to avoid infinite recursive resolving of FactTypes.
    return ObjectUtils.ifNotNull(convertFactType(entity, true), FactType::toInfo);
  }
}
