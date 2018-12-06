package no.mnemonic.act.platform.service.ti.handlers;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ScrollingSearchResult;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handler class implementing search for Facts.
 */
public class FactSearchHandler {

  private static final int MAXIMUM_SEARCH_LIMIT = 10_000;
  private static final int MAXIMUM_BATCH_SIZE = 1000;

  private static final Logger LOGGER = Logging.getLogger(FactSearchHandler.class);

  private final FactTypeResolver factTypeResolver;
  private final FactSearchManager factSearchManager;
  private final FactManager factManager;
  private final TiSecurityContext securityContext;
  private final Function<FactEntity, Fact> factConverter;

  private FactSearchHandler(FactTypeResolver factTypeResolver,
                            FactSearchManager factSearchManager,
                            FactManager factManager,
                            TiSecurityContext securityContext,
                            Function<FactEntity, Fact> factConverter) {
    this.factTypeResolver = factTypeResolver;
    this.factSearchManager = factSearchManager;
    this.factManager = factManager;
    this.securityContext = securityContext;
    this.factConverter = factConverter;
  }

  /**
   * Search for Facts based on the given {@link FactSearchCriteria}. It will make sure that only Facts a user has
   * access to will be returned.
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
    List<Fact> facts = Streams.stream(new FactsFetchingIterator(factManager, factID))
            .filter(securityContext::hasReadPermission)
            .map(factConverter)
            .limit(limit)
            .collect(Collectors.toList());

    // Note that 'count' might be slightly off when retracted Facts are excluded from the result, because retracted
    // Facts are included in the count from ElasticSearch and are only removed when streaming out the results.
    // However, 'size' will always be correct.
    return ResultSet.<Fact>builder()
            .setCount(searchResult.getCount())
            .setLimit(limit)
            .setValues(facts)
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactTypeResolver factTypeResolver;
    private FactSearchManager factSearchManager;
    private FactManager factManager;
    private TiSecurityContext securityContext;
    private Function<FactEntity, Fact> factConverter;

    private Builder() {
    }

    public FactSearchHandler build() {
      ObjectUtils.notNull(factTypeResolver, "Cannot instantiate FactSearchHandler without 'factTypeResolver'.");
      ObjectUtils.notNull(factSearchManager, "Cannot instantiate FactSearchHandler without 'factSearchManager'.");
      ObjectUtils.notNull(factManager, "Cannot instantiate FactSearchHandler without 'factManager'.");
      ObjectUtils.notNull(securityContext, "Cannot instantiate FactSearchHandler without 'securityContext'.");
      ObjectUtils.notNull(factConverter, "Cannot instantiate FactSearchHandler without 'factConverter'.");
      return new FactSearchHandler(factTypeResolver, factSearchManager, factManager, securityContext, factConverter);
    }

    public Builder setFactTypeResolver(FactTypeResolver factTypeResolver) {
      this.factTypeResolver = factTypeResolver;
      return this;
    }

    public Builder setFactSearchManager(FactSearchManager factSearchManager) {
      this.factSearchManager = factSearchManager;
      return this;
    }

    public Builder setFactManager(FactManager factManager) {
      this.factManager = factManager;
      return this;
    }

    public Builder setSecurityContext(TiSecurityContext securityContext) {
      this.securityContext = securityContext;
      return this;
    }

    public Builder setFactConverter(Function<FactEntity, Fact> factConverter) {
      this.factConverter = factConverter;
      return this;
    }
  }

  private boolean includeRetracted(FactDocument fact, Boolean includeRetracted) {
    // If the document isn't marked as retracted the Fact has never been retracted and should be included in the result.
    if (ObjectUtils.ifNull(includeRetracted, false) || !fact.isRetracted()) {
      return true;
    }

    // Otherwise need to check if the Fact is actually retracted from the user's perspective.
    return !isRetracted(fact);
  }

  private boolean isRetracted(FactDocument fact) {
    List<FactDocument> retractions = fetchRetractions(fact);
    if (CollectionUtils.isEmpty(retractions)) {
      // No accessible retractions, thus, the Fact isn't retracted.
      return false;
    }

    // The Fact is only retracted if not all of the retractions themselves are retracted.
    return !retractions.stream().allMatch(this::isRetracted);
  }

  private List<FactDocument> fetchRetractions(FactDocument fact) {
    // Create criteria to fetch all Retraction Facts for a given Fact. Only return retractions which a user has access
    // to. No access to retractions means that from the user's perspective the referenced Fact isn't retracted.
    FactSearchCriteria retractionsCriteria = FactSearchCriteria.builder()
            .addInReferenceTo(fact.getId())
            .addFactTypeID(factTypeResolver.resolveRetractionFactType().getId())
            .setCurrentUserID(securityContext.getCurrentUserID())
            .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
            .build();

    // The number of retractions will be very small (typically one), thus, it's no problem to consume all results at once.
    return ListUtils.list(factSearchManager.searchFacts(retractionsCriteria));
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
