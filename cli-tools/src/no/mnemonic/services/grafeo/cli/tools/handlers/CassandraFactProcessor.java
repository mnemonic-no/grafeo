package no.mnemonic.services.grafeo.cli.tools.handlers;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactEntity;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Helper class to fetch all Facts from Cassandra within a given time period and perform an operation on each Fact.
 */
class CassandraFactProcessor {

  private static final Logger LOGGER = Logging.getLogger(CassandraFactProcessor.class);
  private static final Duration BUCKET_SIZE = Duration.ofDays(1);

  private final FactManager factManager;

  @Inject
  CassandraFactProcessor(FactManager factManager) {
    this.factManager = factManager;
  }

  /**
   * Fetch all Facts from Cassandra within a given time period and perform an operation on each Fact.
   *
   * @param operation      Operation to perform on each Fact
   * @param startTimestamp Timestamp to start processing Facts
   * @param endTimestamp   Timestamp to stop processing Facts
   * @param reverse        If true reverse the processing order
   */
  void process(Consumer<FactEntity> operation, Instant startTimestamp, Instant endTimestamp, boolean reverse) {
    // Input validation with proper user feedback should be performed by commands, just return here.
    if (endTimestamp.isBefore(startTimestamp)) return;

    if (!reverse) {
      processFromStartToEnd(operation, startTimestamp, endTimestamp);
    } else {
      processFromEndToStart(operation, startTimestamp, endTimestamp);
    }
  }

  private void processFromStartToEnd(Consumer<FactEntity> operation, Instant startTimestamp, Instant endTimestamp) {
    Instant currentBucketStart = startTimestamp;
    Instant currentBucketEnd = advanceCurrentBucketEnd(startTimestamp, endTimestamp);

    // The whole time frame is partitioned into buckets defined by BUCKET_SIZE. Go through all buckets one-by-one.
    while (currentBucketStart.isBefore(endTimestamp)) {
      processFactsWithinCurrentBucket(operation, currentBucketStart, currentBucketEnd);

      // Continue with the next bucket until endTimestamp is reached.
      currentBucketStart = currentBucketEnd;
      currentBucketEnd = advanceCurrentBucketEnd(currentBucketEnd, endTimestamp);
    }
  }

  private void processFromEndToStart(Consumer<FactEntity> operation, Instant startTimestamp, Instant endTimestamp) {
    Instant currentBucketEnd = endTimestamp;
    Instant currentBucketStart = advanceCurrentBucketStart(endTimestamp, startTimestamp);

    // The whole time frame is partitioned into buckets defined by BUCKET_SIZE. Go through all buckets one-by-one.
    while (currentBucketEnd.isAfter(startTimestamp)) {
      processFactsWithinCurrentBucket(operation, currentBucketStart, currentBucketEnd);

      // Continue with the next bucket until startTimestamp is reached.
      currentBucketEnd = currentBucketStart;
      currentBucketStart = advanceCurrentBucketStart(currentBucketStart, startTimestamp);
    }
  }

  private void processFactsWithinCurrentBucket(Consumer<FactEntity> operation, Instant currentBucketStart, Instant currentBucketEnd) {
    LOGGER.info("Process Facts from %s to %s.", currentBucketStart, currentBucketEnd);
    // Fetch all Facts inside one bucket from Cassandra and perform the given operation.
    factManager.getFactsWithin(currentBucketStart.toEpochMilli(), currentBucketEnd.toEpochMilli()).forEachRemaining(operation);
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
}
