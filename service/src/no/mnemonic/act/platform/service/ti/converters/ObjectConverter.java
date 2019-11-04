package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectFactsStatistic;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObjectConverter implements Function<ObjectRecord, Object> {

  private final ObjectTypeByIdConverter objectTypeConverter;
  private final FactTypeByIdConverter factTypeConverter;
  private final Function<UUID, Collection<ObjectStatisticsContainer.FactStatistic>> factStatisticsResolver;

  @Inject
  public ObjectConverter(ObjectTypeByIdConverter objectTypeConverter,
                         FactTypeByIdConverter factTypeConverter,
                         Function<UUID, Collection<ObjectStatisticsContainer.FactStatistic>> factStatisticsResolver) {
    this.objectTypeConverter = objectTypeConverter;
    this.factTypeConverter = factTypeConverter;
    this.factStatisticsResolver = factStatisticsResolver;
  }

  @Override
  public Object apply(ObjectRecord record) {
    if (record == null) return null;
    return Object.builder()
            .setId(record.getId())
            .setType(ObjectUtils.ifNotNull(objectTypeConverter.apply(record.getTypeID()), ObjectType::toInfo))
            .setValue(record.getValue())
            .setStatistics(resolveStatistics(record))
            .build();
  }

  private List<ObjectFactsStatistic> resolveStatistics(ObjectRecord record) {
    Collection<ObjectStatisticsContainer.FactStatistic> statistics = factStatisticsResolver.apply(record.getId());
    if (CollectionUtils.isEmpty(statistics)) {
      return null;
    }

    return statistics.stream()
            .map(stat -> ObjectFactsStatistic.builder()
                    .setType(ObjectUtils.ifNotNull(factTypeConverter.apply(stat.getFactTypeID()), FactType::toInfo))
                    .setCount(stat.getFactCount())
                    .setLastAddedTimestamp(stat.getLastAddedTimestamp())
                    .setLastSeenTimestamp(stat.getLastSeenTimestamp())
                    .build())
            .collect(Collectors.toList());
  }
}
