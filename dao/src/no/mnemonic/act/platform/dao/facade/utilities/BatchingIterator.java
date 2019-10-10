package no.mnemonic.act.platform.dao.facade.utilities;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Wrapper around an {@link Iterator} which uses the input objects returned from the underlying {@link Iterator}
 * to fetch the output objects in a batch-wise manner while iterating over all input objects.
 *
 * @param <I> Type of input objects
 * @param <O> Type of output objects
 */
public class BatchingIterator<I, O> implements Iterator<O> {

  private static final int MAXIMUM_BATCH_SIZE = 1000;

  private final Iterator<I> input;
  private final Function<List<I>, Iterator<O>> nextBatch;
  private Iterator<O> output;

  /**
   * Construct a new instance.
   *
   * @param input     Wrapped iterator (can be null, defaults to an empty iterator)
   * @param nextBatch Function to fetch next output batch (cannot be null)
   */
  public BatchingIterator(Iterator<I> input, Function<List<I>, Iterator<O>> nextBatch) {
    this.input = ObjectUtils.ifNull(input, Collections.emptyIterator());
    this.nextBatch = ObjectUtils.notNull(nextBatch, "'nextBatch' cannot be null!");
  }

  @Override
  public boolean hasNext() {
    advance();
    return output.hasNext();
  }

  @Override
  public O next() {
    advance();
    return output.next();
  }

  private void advance() {
    // If this is the initial batch or the current batch has be consumed completely, fetch the next batch.
    if (output == null || !output.hasNext()) {
      output = nextOutputBatch();
    }
  }

  private Iterator<O> nextOutputBatch() {
    List<I> next = new ArrayList<>(MAXIMUM_BATCH_SIZE);

    int currentBatchSize = 0;
    // Consume input until no more data is available or maximum batch size has be reached.
    while (input.hasNext() && currentBatchSize < MAXIMUM_BATCH_SIZE) {
      next.add(input.next());
      currentBatchSize++;
    }

    if (next.isEmpty()) {
      // All input elements have been consumed, iteration of output elements should be stopped.
      return Collections.emptyIterator();
    }

    // Fetch next output batch.
    return ObjectUtils.ifNull(nextBatch.apply(next), Collections.emptyIterator());
  }
}
