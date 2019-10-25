package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactRecordConverter;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.Iterator;

/**
 * Handler class implementing search for Facts.
 */
public class FactSearchHandler {

  private static final int MAXIMUM_SEARCH_LIMIT = 10_000;

  private final FactRetractionHandler retractionHandler;
  private final ObjectFactDao objectFactDao;
  private final TiSecurityContext securityContext;
  private final FactRecordConverter factConverter;

  @Inject
  public FactSearchHandler(FactRetractionHandler retractionHandler,
                           ObjectFactDao objectFactDao,
                           TiSecurityContext securityContext,
                           FactRecordConverter factConverter) {
    this.retractionHandler = retractionHandler;
    this.objectFactDao = objectFactDao;
    this.securityContext = securityContext;
    this.factConverter = factConverter;
  }

  /**
   * Search for Facts based on the given {@link FactSearchCriteria}. It will make sure that only Facts a user has
   * access to will be returned. Facts are streamed out from the database while the returned ResultSet is consumed.
   *
   * @param criteria         Search criteria matched against existing Facts
   * @param includeRetracted Whether retracted Facts should be included in the result (false by default)
   * @return Facts wrapped inside a ResultSet
   */
  public ResultSet<Fact> search(FactSearchCriteria criteria, Boolean includeRetracted) {
    // Restrict the number of results returned from search. Right now everything above MAXIMUM_SEARCH_LIMIT will put too
    // much load onto the application and will be very slow. Implements same logic as previously done in ElasticSearch.
    int limit = criteria.getLimit() > 0 && criteria.getLimit() < MAXIMUM_SEARCH_LIMIT ? criteria.getLimit() : MAXIMUM_SEARCH_LIMIT;
    ResultContainer<FactRecord> searchResult = objectFactDao.searchFacts(criteria);

    // When consuming the search result apply filter to include or exclude retracted Facts.
    // Additionally, make sure that the user has access to all returned Facts.
    Iterator<Fact> facts = searchResult.stream()
            .filter(fact -> includeRetracted(fact, includeRetracted))
            .filter(securityContext::hasReadPermission)
            .map(factConverter)
            .limit(limit)
            .iterator();

    // Note that 'count' might be slightly off when retracted Facts are excluded from the result, because retracted
    // Facts are included in the count from ElasticSearch and are only removed when streaming out the results.
    // However, 'size' will always be correct.
    return StreamingResultSet.<Fact>builder()
            .setCount(searchResult.getCount())
            .setLimit(limit)
            .setValues(facts)
            .build();
  }

  private boolean includeRetracted(FactRecord fact, Boolean includeRetracted) {
    // Call FactRetractionHandler for every Fact in order to populate the cache which is re-used by the converter.
    // Because of that, it's only calculated once whether a Fact is retracted from the user's point of view.
    boolean retractedHint = SetUtils.set(fact.getFlags()).contains(FactRecord.Flag.RetractedHint);
    boolean isRetracted = retractionHandler.isRetracted(fact.getId(), retractedHint);
    return ObjectUtils.ifNull(includeRetracted, false) || !isRetracted;
  }
}
