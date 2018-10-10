package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactTypeConverter implements Converter<FactTypeEntity, FactType> {

  private final Function<UUID, Namespace> namespaceConverter;
  private final Function<UUID, ObjectType> objectTypeConverter;

  private FactTypeConverter(Function<UUID, Namespace> namespaceConverter, Function<UUID, ObjectType> objectTypeConverter) {
    this.namespaceConverter = namespaceConverter;
    this.objectTypeConverter = objectTypeConverter;
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
    if (entity == null) return null;
    return FactType.builder()
            .setId(entity.getId())
            .setNamespace(namespaceConverter.apply(entity.getNamespaceID()))
            .setName(entity.getName())
            .setValidator(entity.getValidator())
            .setValidatorParameter(entity.getValidatorParameter())
            .setRelevantObjectBindings(convertBindings(entity.getRelevantObjectBindings()))
            .build();
  }

  private List<FactType.FactObjectBindingDefinition> convertBindings(Set<FactTypeEntity.FactObjectBindingDefinition> bindings) {
    if (bindings == null) return null;
    return bindings.stream()
            .map(e -> new FactType.FactObjectBindingDefinition(
                    ObjectUtils.ifNotNull(e.getSourceObjectTypeID(), id -> objectTypeConverter.apply(id).toInfo()),
                    ObjectUtils.ifNotNull(e.getDestinationObjectTypeID(), id -> objectTypeConverter.apply(id).toInfo()),
                    e.isBidirectionalBinding()))
            .collect(Collectors.toList());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Function<UUID, Namespace> namespaceConverter;
    private Function<UUID, ObjectType> objectTypeConverter;

    private Builder() {
    }

    public FactTypeConverter build() {
      ObjectUtils.notNull(namespaceConverter, "Cannot instantiate FactTypeConverter without 'namespaceConverter'.");
      ObjectUtils.notNull(objectTypeConverter, "Cannot instantiate FactTypeConverter without 'objectTypeConverter'.");
      return new FactTypeConverter(namespaceConverter, objectTypeConverter);
    }

    public Builder setNamespaceConverter(Function<UUID, Namespace> namespaceConverter) {
      this.namespaceConverter = namespaceConverter;
      return this;
    }

    public Builder setObjectTypeConverter(Function<UUID, ObjectType> objectTypeConverter) {
      this.objectTypeConverter = objectTypeConverter;
      return this;
    }
  }

}
