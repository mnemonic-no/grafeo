package no.mnemonic.act.platform.dao.elastic.document;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;

/**
 * Container streaming out the results of a search from ElasticSearch, i.e. the matching documents. The container
 * implements {@link Iterator} and fetches search results batch-wise until no more results are available.
 *
 * @param <T> Type of result values
 */
public class ScrollingSearchResult<T extends ElasticDocument> implements Iterator<T> {

  private final Function<String, ScrollingBatch<T>> fetchNextBatch;
  private final int count;

  private ScrollingBatch<T> currentBatch;

  private ScrollingSearchResult(ScrollingBatch<T> initialBatch, Function<String, ScrollingBatch<T>> fetchNextBatch, int count) {
    this.currentBatch = ObjectUtils.notNull(initialBatch, "'initialBatch' cannot be null!");
    this.fetchNextBatch = ObjectUtils.notNull(fetchNextBatch, "'fetchNextBatch' cannot be null!");
    this.count = count;
  }

  @Override
  public boolean hasNext() {
    if (!currentBatch.hasNext() && !currentBatch.isFinished()) {
      // If the current batch has been consumed completely and there's more data available, fetch the next batch.
      currentBatch = ObjectUtils.notNull(fetchNextBatch.apply(currentBatch.getScrollId()), "'currentBatch' cannot be null!");
    }

    return currentBatch.hasNext();
  }

  @Override
  public T next() {
    return currentBatch.next();
  }

  /**
   * Returns the total number of indexed documents matching a given search criteria, i.e. the amount of available search results.
   *
   * @return Total number of matching documents
   */
  public int getCount() {
    return count;
  }

  /**
   * Returns an empty batch of results which is marked as finished.
   *
   * @param <T> Type of result values
   * @return Empty result batch
   */
  public static <T extends ElasticDocument> ScrollingBatch<T> emptyBatch() {
    return new ScrollingBatch<>("EMPTY_SCROLLING_BATCH", Collections.emptyIterator(), true);
  }

  /**
   * Creates a builder for ScrollingSearchResult.
   *
   * @param <T> Type of result values
   * @return New builder
   */
  public static <T extends ElasticDocument> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Builder for creating a new ScrollingSearchResult.
   *
   * @param <T> Type of result values
   */
  public static class Builder<T extends ElasticDocument> {
    private ScrollingBatch<T> initialBatch = emptyBatch();
    private Function<String, ScrollingBatch<T>> fetchNextBatch = scrollId -> emptyBatch();
    private int count;

    private Builder() {
    }

    /**
     * Creates a new ScrollingSearchResult.
     *
     * @return New ScrollingSearchResult
     */
    public ScrollingSearchResult<T> build() {
      return new ScrollingSearchResult<>(initialBatch, fetchNextBatch, count);
    }

    /**
     * Sets first batch of search results.
     *
     * @param initialBatch First batch of search results
     * @return This builder
     */
    public Builder<T> setInitialBatch(ScrollingBatch<T> initialBatch) {
      this.initialBatch = initialBatch;
      return this;
    }

    /**
     * Sets function to fetch next batch of search results.
     * <p>
     * It takes an ElasticSearch scroll ID as input and should return the next batch of search results.
     *
     * @param fetchNextBatch Function to fetch next batch of search results
     * @return This builder
     */
    public Builder<T> setFetchNextBatch(Function<String, ScrollingBatch<T>> fetchNextBatch) {
      this.fetchNextBatch = fetchNextBatch;
      return this;
    }

    /**
     * Sets the total number of indexed documents matching a given search criteria, i.e. the amount of available search results.
     *
     * @param count Total number of matching documents
     * @return This builder
     */
    public Builder<T> setCount(int count) {
      this.count = count;
      return this;
    }
  }

  /**
   * Container holding the search results of one batch.
   *
   * @param <T> Type of result values
   */
  public static class ScrollingBatch<T> implements Iterator<T> {
    private final String scrollId;
    private final Iterator<T> values;
    private final boolean finished;

    /**
     * Creates a new batch of search results.
     *
     * @param scrollId Scroll ID returned from ElasticSearch
     * @param values   Search results from current batch
     * @param finished Whether more data is available in ElasticSearch
     */
    public ScrollingBatch(String scrollId, Iterator<T> values, boolean finished) {
      this.scrollId = ObjectUtils.notNull(scrollId, "'scrollId' cannot be null!");
      this.values = ObjectUtils.notNull(values, "'values' cannot be null!");
      this.finished = finished;
    }

    /**
     * Returns the scroll ID from ElasticSearch required for fetching more batches.
     *
     * @return Scroll ID
     */
    String getScrollId() {
      return scrollId;
    }

    /**
     * Returns true if no more data is available from ElasticSearch, i.e. all search results have been fetched.
     *
     * @return True if all search results have been fetched
     */
    boolean isFinished() {
      return finished;
    }

    @Override
    public boolean hasNext() {
      return values.hasNext();
    }

    @Override
    public T next() {
      return values.next();
    }
  }
}
