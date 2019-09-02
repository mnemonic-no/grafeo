package no.mnemonic.act.platform.dao.cassandra.utilities;

import com.google.common.collect.Lists;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * {@link Iterator} useful for implementing multi-fetch against Cassandra. It breaks down a list of UUID into multiple
 * batches which will be fetched from Cassandra one-by-one. This gives better performance in the case an IN-clause is
 * used to implement multi-fetch, or when not all results are actually consumed (batches are fetched on-demand).
 *
 * @param <T> Type of returned elements
 */
public class MultiFetchIterator<T> implements Iterator<T> {

  private static final Logger LOGGER = Logging.getLogger(MultiFetchIterator.class);
  // Cassandra's performance is poor with a lot of elements inside an IN-clause, thus, use a small batch size.
  private static final int BATCH_SIZE = 100;

  private final Function<List<UUID>, Iterator<T>> nextBatch;
  private final List<List<UUID>> partitions;
  private Iterator<T> currentBatch;
  private int nextPartition;

  /**
   * Create a new instance.
   *
   * @param nextBatch Function for fetching the next batch of elements
   * @param id        List of all elements to fetch (by id)
   */
  public MultiFetchIterator(Function<List<UUID>, Iterator<T>> nextBatch, List<UUID> id) {
    this.nextBatch = ObjectUtils.notNull(nextBatch, "'nextBatch' cannot be null!");
    // The input for multi-fetch is a list of ids. This list can be large, so split it up into smaller partitions
    // which will be fetched one-by-one.
    this.partitions = Lists.partition(ObjectUtils.notNull(id, "'id' cannot be null!"), BATCH_SIZE);
  }

  @Override
  public boolean hasNext() {
    // If this is the initial batch or the current batch has be consumed completely, fetch the next batch.
    if (currentBatch == null || !currentBatch.hasNext()) {
      currentBatch = ObjectUtils.notNull(nextBatch(), "Next batch cannot be null!");
      LOGGER.debug("Successfully fetched next batch from Cassandra.");
    }

    return currentBatch.hasNext();
  }

  @Override
  public T next() {
    return currentBatch.next();
  }

  private Iterator<T> nextBatch() {
    if (partitions.size() > nextPartition) {
      // Fetch the next batch of elements for the next partition of ids.
      Iterator<T> batch = nextBatch.apply(partitions.get(nextPartition));
      nextPartition++;
      return batch;
    }

    // All partitions have been fetched. Just return an empty iterator.
    return Collections.emptyIterator();
  }
}
