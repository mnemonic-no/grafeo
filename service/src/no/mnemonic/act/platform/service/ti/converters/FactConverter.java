package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactConverter implements Converter<FactEntity, Fact> {

  private final Function<UUID, FactType> factTypeConverter;
  private final Function<UUID, Fact> inReferenceToConverter;
  private final Function<UUID, Organization> organizationConverter;
  private final Function<UUID, Source> sourceConverter;
  private final Function<UUID, Object> objectConverter;

  private FactConverter(Function<UUID, FactType> factTypeConverter, Function<UUID, Fact> inReferenceToConverter,
                        Function<UUID, Organization> organizationConverter, Function<UUID, Source> sourceConverter,
                        Function<UUID, Object> objectConverter) {
    this.factTypeConverter = factTypeConverter;
    this.inReferenceToConverter = inReferenceToConverter;
    this.organizationConverter = organizationConverter;
    this.sourceConverter = sourceConverter;
    this.objectConverter = objectConverter;
  }

  @Override
  public Class<FactEntity> getSourceType() {
    return FactEntity.class;
  }

  @Override
  public Class<Fact> getTargetType() {
    return Fact.class;
  }

  @Override
  public Fact apply(FactEntity entity) {
    if (entity == null) return null;
    return Fact.builder()
            .setId(entity.getId())
            .setType(factTypeConverter.apply(entity.getTypeID()).toInfo())
            .setValue(entity.getValue())
            .setInReferenceTo(ObjectUtils.ifNotNull(inReferenceToConverter.apply(entity.getInReferenceToID()), Fact::toInfo))
            .setOrganization(ObjectUtils.ifNotNull(organizationConverter.apply(entity.getOrganizationID()), Organization::toInfo))
            .setSource(ObjectUtils.ifNotNull(sourceConverter.apply(entity.getSourceID()), Source::toInfo))
            .setAccessMode(AccessMode.valueOf(entity.getAccessMode().name()))
            .setTimestamp(entity.getTimestamp())
            .setLastSeenTimestamp(entity.getLastSeenTimestamp())
            .setObjects(convertBindings(entity.getBindings()))
            .build();
  }

  private List<Fact.FactObjectBinding> convertBindings(List<FactEntity.FactObjectBinding> bindings) {
    if (bindings == null) return null;
    return bindings.stream()
            .map(e -> new Fact.FactObjectBinding(
                    objectConverter.apply(e.getObjectID()).toInfo(),
                    Direction.valueOf(e.getDirection().name())))
            .collect(Collectors.toList());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Function<UUID, FactType> factTypeConverter;
    private Function<UUID, Fact> inReferenceToConverter;
    private Function<UUID, Organization> organizationConverter;
    private Function<UUID, Source> sourceConverter;
    private Function<UUID, Object> objectConverter;

    private Builder() {
    }

    public FactConverter build() {
      ObjectUtils.notNull(factTypeConverter, "Cannot instantiate FactConverter without 'factTypeConverter'.");
      ObjectUtils.notNull(inReferenceToConverter, "Cannot instantiate FactConverter without 'inReferenceToConverter'.");
      ObjectUtils.notNull(organizationConverter, "Cannot instantiate FactConverter without 'organizationConverter'.");
      ObjectUtils.notNull(sourceConverter, "Cannot instantiate FactConverter without 'sourceConverter'.");
      ObjectUtils.notNull(objectConverter, "Cannot instantiate FactConverter without 'objectConverter'.");
      return new FactConverter(factTypeConverter, inReferenceToConverter, organizationConverter, sourceConverter, objectConverter);
    }

    public Builder setFactTypeConverter(Function<UUID, FactType> factTypeConverter) {
      this.factTypeConverter = factTypeConverter;
      return this;
    }

    public Builder setInReferenceToConverter(Function<UUID, Fact> inReferenceToConverter) {
      this.inReferenceToConverter = inReferenceToConverter;
      return this;
    }

    public Builder setOrganizationConverter(Function<UUID, Organization> organizationConverter) {
      this.organizationConverter = organizationConverter;
      return this;
    }

    public Builder setSourceConverter(Function<UUID, Source> sourceConverter) {
      this.sourceConverter = sourceConverter;
      return this;
    }

    public Builder setObjectConverter(Function<UUID, Object> objectConverter) {
      this.objectConverter = objectConverter;
      return this;
    }
  }

}
