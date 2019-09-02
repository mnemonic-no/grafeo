package no.mnemonic.act.platform.service.ti.utilities;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Iterator which consumes a stream of Fact UUIDs and uses those UUIDs to fetch Facts from Cassandra.
 * Data is fetched batch-wise from Cassandra while iterating over the input stream.
 */
public class FactsFetchingIterator implements Iterator<FactEntity> {

  private static final Logger LOGGER = Logging.getLogger(FactsFetchingIterator.class);
  private static final int MAXIMUM_BATCH_SIZE = 1000;

  private final FactManager factManager;
  private final Iterator<UUID> input;
  private Iterator<FactEntity> output;

  /**
   * Create a new instance.
   *
   * @param factManager FactManager
   * @param input       Iterator of Fact UUIDs
   */
  public FactsFetchingIterator(FactManager factManager, Iterator<UUID> input) {
    this.factManager = ObjectUtils.notNull(factManager, "'factManager' cannot be null!");
    this.input = ObjectUtils.notNull(input, "'input' cannot be null!");
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
