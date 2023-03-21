package no.mnemonic.services.grafeo.service.ti.converters.request;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.v1.TimeFieldSearchRequest;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;

import java.util.Set;

/**
 * This class contains utilities for converting request objects.
 */
public class RequestConverterUtils {

  private RequestConverterUtils() {
  }

  /**
   * Convert the fields of a {@link TimeFieldSearchRequest} and sets them on a given {@link FactSearchCriteria.Builder}.
   *
   * @param criteriaBuilder {@link FactSearchCriteria.Builder} to modify
   * @param searchRequest   {@link TimeFieldSearchRequest} to convert
   * @return Modified {@link FactSearchCriteria.Builder}
   */
  public static FactSearchCriteria.Builder handleTimeFieldSearchRequest(FactSearchCriteria.Builder criteriaBuilder, TimeFieldSearchRequest searchRequest) {
    Set<FactSearchCriteria.TimeFieldStrategy> timeFieldStrategy = !CollectionUtils.isEmpty(searchRequest.getTimeFieldStrategy()) ?
            SetUtils.set(searchRequest.getTimeFieldStrategy(), strategy -> FactSearchCriteria.TimeFieldStrategy.valueOf(strategy.name())) :
            SetUtils.set(FactSearchCriteria.TimeFieldStrategy.lastSeenTimestamp);

    return criteriaBuilder
            .setStartTimestamp(searchRequest.getStartTimestamp())
            .setEndTimestamp(searchRequest.getEndTimestamp())
            .setTimeMatchStrategy(ObjectUtils.ifNotNull(searchRequest.getTimeMatchStrategy(),
                    strategy -> FactSearchCriteria.MatchStrategy.valueOf(strategy.name()),
                    FactSearchCriteria.MatchStrategy.any))
            .setTimeFieldStrategy(timeFieldStrategy);
  }
}
