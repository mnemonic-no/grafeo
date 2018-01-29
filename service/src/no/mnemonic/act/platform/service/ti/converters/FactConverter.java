package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FactConverter implements Converter<FactEntity, Fact> {

  private final Function<UUID, FactType> factTypeConverter;
  private final Function<UUID, Organization> organizationConverter;
  private final Function<UUID, Source> sourceConverter;
  private final Function<UUID, Object> objectConverter;
  private final Function<UUID, FactEntity> factEntityResolver;
  private final Predicate<FactEntity> accessChecker;

  private FactConverter(Function<UUID, FactType> factTypeConverter, Function<UUID, Organization> organizationConverter,
                        Function<UUID, Source> sourceConverter, Function<UUID, Object> objectConverter,
                        Function<UUID, FactEntity> factEntityResolver, Predicate<FactEntity> accessChecker) {
    this.factTypeConverter = factTypeConverter;
    this.organizationConverter = organizationConverter;
    this.sourceConverter = sourceConverter;
    this.objectConverter = objectConverter;
    this.factEntityResolver = factEntityResolver;
    this.accessChecker = accessChecker;
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
            .setInReferenceTo(ObjectUtils.ifNotNull(convertInReferenceTo(entity.getInReferenceToID()), Fact::toInfo))
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

  private Fact convertInReferenceTo(UUID inReferenceToID) {
    if (inReferenceToID == null) return null;

    FactEntity inReferenceTo = factEntityResolver.apply(inReferenceToID);
    if (inReferenceTo == null || !accessChecker.test(inReferenceTo)) {
      // If User doesn't have access to 'inReferenceTo' Fact it shouldn't be returned as part of the converted Fact.
      return null;
    }

    // Convert 'inReferenceTo' Fact, but avoid resolving recursive 'inReferenceTo' Facts.
    // Clone entity first in order to not disturb DAO layer.
    return apply(inReferenceTo.clone().setInReferenceToID(null));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Function<UUID, FactType> factTypeConverter;
    private Function<UUID, Organization> organizationConverter;
    private Function<UUID, Source> sourceConverter;
    private Function<UUID, Object> objectConverter;
    private Function<UUID, FactEntity> factEntityResolver;
    private Predicate<FactEntity> accessChecker;

    private Builder() {
    }

    public FactConverter build() {
      ObjectUtils.notNull(factTypeConverter, "Cannot instantiate FactConverter without 'factTypeConverter'.");
      ObjectUtils.notNull(organizationConverter, "Cannot instantiate FactConverter without 'organizationConverter'.");
      ObjectUtils.notNull(sourceConverter, "Cannot instantiate FactConverter without 'sourceConverter'.");
      ObjectUtils.notNull(objectConverter, "Cannot instantiate FactConverter without 'objectConverter'.");
      ObjectUtils.notNull(factEntityResolver, "Cannot instantiate FactConverter without 'factEntityResolver'.");
      ObjectUtils.notNull(accessChecker, "Cannot instantiate FactConverter without 'accessChecker'.");
      return new FactConverter(factTypeConverter, organizationConverter, sourceConverter, objectConverter, factEntityResolver, accessChecker);
    }

    public Builder setFactTypeConverter(Function<UUID, FactType> factTypeConverter) {
      this.factTypeConverter = factTypeConverter;
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

    public Builder setFactEntityResolver(Function<UUID, FactEntity> factEntityResolver) {
      this.factEntityResolver = factEntityResolver;
      return this;
    }

    public Builder setAccessChecker(Predicate<FactEntity> accessChecker) {
      this.accessChecker = accessChecker;
      return this;
    }
  }

}
