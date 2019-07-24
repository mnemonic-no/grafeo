package no.mnemonic.act.platform.service.ti.handlers;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ScrollingSearchResult;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Handler class implementing search for Facts.
 */
public class FactSearchHandler {

  private static final int MAXIMUM_SEARCH_LIMIT = 10_000;
  private static final int MAXIMUM_BATCH_SIZE = 1000;

  private static final Logger LOGGER = Logging.getLogger(FactSearchHandler.class);

  private final FactRetractionHandler retractionHandler;
  private final FactSearchManager factSearchManager;
  private final FactManager factManager;
  private final TiSecurityContext securityContext;
  private final Function<FactEntity, Fact> factConverter;

  @Inject
  public FactSearchHandler(FactRetractionHandler retractionHandler,
                           FactSearchManager factSearchManager,
                           FactManager factManager,
                           TiSecurityContext securityContext,
                           Function<FactEntity, Fact> factConverter) {
    this.retractionHandler = retractionHandler;
    this.factSearchManager = factSearchManager;
    this.factManager = factManager;
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

    // Search for Facts in ElasticSearch, stream out documents and pick out all Fact IDs.
    // Also, apply filter to include or exclude retracted Fact.
    ScrollingSearchResult<FactDocument> searchResult = factSearchManager.searchFacts(criteria);
    Iterator<UUID> factID = Streams.stream(searchResult)
            .filter(fact -> includeRetracted(fact, includeRetracted))
            .map(FactDocument::getId)
            .iterator();

    // Use the Fact IDs to look up the authoritative data in Cassandra, and make sure that user has access to all
    // returned Facts. Facts are fetched batch-wise from Cassandra while iterating over the result from ElasticSearch.
    Iterator<Fact> facts = Streams.stream(new FactsFetchingIterator(factManager, factID))
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

  private boolean includeRetracted(FactDocument fact, Boolean includeRetracted) {
    // Call FactRetractionHandler for every Fact in order to populate the cache which is re-used by the FactConverter.
    // On FactDocument it's stored whether a Fact has ever been retracted. Populating the cache with this information
    // here saves a lot of calls to ElasticSearch by the FactConverter which doesn't have the same information (as it's
    // not stored on FactEntity).
    boolean isRetracted = retractionHandler.isRetracted(fact.getId(), fact.isRetracted());
    return ObjectUtils.ifNull(includeRetracted, false) || !isRetracted;
  }

  /**
   * Iterator which consumes a stream of Fact UUIDs and uses those UUIDs to fetch Facts from Cassandra.
   * Data is fetched batch-wise from Cassandra while iterating over the input stream.
   */
  private class FactsFetchingIterator implements Iterator<FactEntity> {
    private final FactManager factManager;
    private final Iterator<UUID> input;
    private Iterator<FactEntity> output;

    private FactsFetchingIterator(FactManager factManager, Iterator<UUID> input) {
      this.factManager = factManager;
      this.input = input;
    }

    @Override
    public boolean hasNext() {
      // If this is the initial batch or the current batch has be consumed completely, fetch the next batch.
      if (output == null || !output.hasNext()) {
        output = ObjectUtils.notNull(nextOutputBatch(), "Next output batch cannot be null!");
        LOGGER.debug("Successfully fetched next batch of Facts from Cassandra.");
      }

      return output.hasNext();
    }

    @Override
    public FactEntity next() {
      return output.next();
    }

    private Iterator<FactEntity> nextOutputBatch() {
      List<UUID> factID = new ArrayList<>(MAXIMUM_BATCH_SIZE);

      int currentBatchSize = 0;
      // Consume input until no more data is available or maximum batch size has be reached.
      while (input.hasNext() && currentBatchSize < MAXIMUM_BATCH_SIZE) {
        factID.add(input.next());
        currentBatchSize++;
      }

      // Fetch next batch of entities. This will return an empty iterator if 'factID' is empty.
      return factManager.getFacts(factID);
    }
  }
}
