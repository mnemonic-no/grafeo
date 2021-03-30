package no.mnemonic.act.platform.cli.tools.handlers;

import no.mnemonic.act.platform.cli.tools.converters.FactEntityToDocumentConverter;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

@Singleton
public class CassandraToElasticSearchReindexHandler {

  private static final Logger LOGGER = Logging.getLogger(CassandraToElasticSearchReindexHandler.class);
  private static final Duration BUCKET_SIZE = Duration.ofDays(1);

  @Dependency
  private final FactManager factManager;
  @Dependency
  private final FactSearchManager factSearchManager;

  private final FactEntityToDocumentConverter factConverter;

  @Inject
  public CassandraToElasticSearchReindexHandler(
          FactManager factManager,
          FactSearchManager factSearchManager,
          FactEntityToDocumentConverter factConverter
  ) {
    this.factManager = factManager;
    this.factSearchManager = factSearchManager;
    this.factConverter = factConverter;
  }

  /**
   * Reindex Facts from Cassandra into ElasticSearch.
   * <p>
   * Fetches all Facts created between startTimestamp and endTimestamp from Cassandra and indexes them into ElasticSearch.
   *
   * @param startTimestamp Timestamp to start reindexing
   * @param endTimestamp   Timestamp to stop reindexing
   * @param reverse        If true reverse the reindexing order
   */
  public void reindex(Instant startTimestamp, Instant endTimestamp, boolean reverse) {
    // Input validation with proper user feedback is performed by ReindexCommand, just return here.
    if (endTimestamp.isBefore(startTimestamp)) return;

    LOGGER.info("Reindex Facts between %s and %s.", startTimestamp, endTimestamp);

    LongAdder processedFacts = new LongAdder();
    if (!reverse) {
      reindexFromStartToEnd(startTimestamp, endTimestamp, processedFacts);
    } else {
      reindexFromEndToStart(startTimestamp, endTimestamp, processedFacts);
    }

    LOGGER.info("Finished reindexing Facts, processed %d Facts in total.", processedFacts.longValue());
  }

  private void reindexFromStartToEnd(Instant startTimestamp, Instant endTimestamp, LongAdder processedFacts) {
    Instant currentBucketStart = startTimestamp;
    Instant currentBucketEnd = advanceCurrentBucketEnd(startTimestamp, endTimestamp);

    // The whole time frame is partitioned into buckets defined by BUCKET_SIZE. Go through all buckets one-by-one.
    while (currentBucketStart.isBefore(endTimestamp)) {
      reindexFactsWithinCurrentBucket(currentBucketStart, currentBucketEnd, processedFacts);

      // Continue with the next bucket until endTimestamp is reached.
      currentBucketStart = currentBucketEnd;
      currentBucketEnd = advanceCurrentBucketEnd(currentBucketEnd, endTimestamp);
    }
  }

  private void reindexFromEndToStart(Instant startTimestamp, Instant endTimestamp, LongAdder processedFacts) {
    Instant currentBucketEnd = endTimestamp;
    Instant currentBucketStart = advanceCurrentBucketStart(endTimestamp, startTimestamp);

    // The whole time frame is partitioned into buckets defined by BUCKET_SIZE. Go through all buckets one-by-one.
    while (currentBucketEnd.isAfter(startTimestamp)) {
      reindexFactsWithinCurrentBucket(currentBucketStart, currentBucketEnd, processedFacts);

      // Continue with the next bucket until startTimestamp is reached.
      currentBucketEnd = currentBucketStart;
      currentBucketStart = advanceCurrentBucketStart(currentBucketStart, startTimestamp);
    }
  }

  private Instant advanceCurrentBucketEnd(Instant currentBucketEnd, Instant endTimestamp) {
    Instant newEnd = currentBucketEnd.plus(BUCKET_SIZE);
    if (newEnd.isAfter(endTimestamp)) {
      newEnd = endTimestamp;
    }

    return newEnd;
  }

  private Instant advanceCurrentBucketStart(Instant currentBucketStart, Instant startTimestamp) {
    Instant newStart = currentBucketStart.minus(BUCKET_SIZE);
    if (newStart.isBefore(startTimestamp)) {
      newStart = startTimestamp;
    }

    return newStart;
  }

  private void reindexFactsWithinCurrentBucket(Instant currentBucketStart, Instant currentBucketEnd, LongAdder processedFacts) {
    LOGGER.info("Process Facts from %s to %s.", currentBucketStart, currentBucketEnd);
    // Fetch all Facts inside one bucket from Cassandra, convert them to documents and index them into ElasticSearch.
    factManager.getFactsWithin(currentBucketStart.toEpochMilli(), currentBucketEnd.toEpochMilli())
            .forEachRemaining(fact -> {
              factSearchManager.indexFact(factConverter.apply(fact));
              processedFacts.increment();
            });
  }
}
