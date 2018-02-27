package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectFactsStatistic;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsResult;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObjectConverter implements Converter<ObjectEntity, Object> {

  private final Function<UUID, ObjectType> objectTypeConverter;
  private final Function<UUID, FactType> factTypeConverter;
  private final Function<UUID, Collection<ObjectStatisticsResult.FactStatistic>> factStatisticsResolver;

  private ObjectConverter(Function<UUID, ObjectType> objectTypeConverter, Function<UUID, FactType> factTypeConverter,
                          Function<UUID, Collection<ObjectStatisticsResult.FactStatistic>> factStatisticsResolver) {
    this.objectTypeConverter = objectTypeConverter;
    this.factTypeConverter = factTypeConverter;
    this.factStatisticsResolver = factStatisticsResolver;
  }

  @Override
  public Class<ObjectEntity> getSourceType() {
    return ObjectEntity.class;
  }

  @Override
  public Class<Object> getTargetType() {
    return Object.class;
  }

  @Override
  public Object apply(ObjectEntity entity) {
    if (entity == null) return null;
    return Object.builder()
            .setId(entity.getId())
            .setType(objectTypeConverter.apply(entity.getTypeID()).toInfo())
            .setValue(entity.getValue())
            .setStatistics(resolveStatistics(entity))
            .build();
  }

  private List<ObjectFactsStatistic> resolveStatistics(ObjectEntity entity) {
    Collection<ObjectStatisticsResult.FactStatistic> statistics = factStatisticsResolver.apply(entity.getId());
    if (CollectionUtils.isEmpty(statistics)) {
      return null;
    }

    return statistics.stream()
            .map(stat -> ObjectFactsStatistic.builder()
                    .setType(factTypeConverter.apply(stat.getFactTypeID()).toInfo())
                    .setCount(stat.getFactCount())
                    .setLastAddedTimestamp(stat.getLastAddedTimestamp())
                    .setLastSeenTimestamp(stat.getLastSeenTimestamp())
                    .build())
            .collect(Collectors.toList());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Function<UUID, ObjectType> objectTypeConverter;
    private Function<UUID, FactType> factTypeConverter;
    private Function<UUID, Collection<ObjectStatisticsResult.FactStatistic>> factStatisticsResolver;

    private Builder() {
    }

    public ObjectConverter build() {
      ObjectUtils.notNull(objectTypeConverter, "Cannot instantiate ObjectConverter without 'objectTypeConverter'.");
      ObjectUtils.notNull(factTypeConverter, "Cannot instantiate ObjectConverter without 'factTypeConverter'.");
      ObjectUtils.notNull(factStatisticsResolver, "Cannot instantiate ObjectConverter without 'factStatisticsResolver'.");
      return new ObjectConverter(objectTypeConverter, factTypeConverter, factStatisticsResolver);
    }

    public Builder setObjectTypeConverter(Function<UUID, ObjectType> objectTypeConverter) {
      this.objectTypeConverter = objectTypeConverter;
      return this;
    }

    public Builder setFactTypeConverter(Function<UUID, FactType> factTypeConverter) {
      this.factTypeConverter = factTypeConverter;
      return this;
    }

    public Builder setFactStatisticsResolver(Function<UUID, Collection<ObjectStatisticsResult.FactStatistic>> factStatisticsResolver) {
      this.factStatisticsResolver = factStatisticsResolver;
      return this;
    }
  }

}
