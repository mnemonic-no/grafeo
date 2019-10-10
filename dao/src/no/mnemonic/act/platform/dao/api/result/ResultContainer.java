package no.mnemonic.act.platform.dao.api.result;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Collections;
import java.util.Iterator;

/**
 * General container holding search results. It is backed by an {@link Iterator} in order to support streaming
 * of search results. Because of that, the number of results cannot be determined without consuming all values.
 *
 * @param <T> Type of result values
 */
public class ResultContainer<T> implements Iterable<T>, Iterator<T> {

  private final int count;
  private final Iterator<T> values;

  private ResultContainer(int count, Iterator<T> values) {
    this.count = count;
    this.values = ObjectUtils.ifNull(values, Collections.emptyIterator());
  }

  /**
   * Number of available search results. This can be larger than the actual number of returned results if the search
   * has been limited by the caller.
   *
   * @return Number of available search results
   */
  public int getCount() {
    return count;
  }

  @Override
  public Iterator<T> iterator() {
    return values;
  }

  @Override
  public boolean hasNext() {
    return values.hasNext();
  }

  @Override
  public T next() {
    return values.next();
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {
    private int count;
    private Iterator<T> values;

    private Builder() {
    }

    public ResultContainer<T> build() {
      return new ResultContainer<>(count, values);
    }

    public Builder<T> setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder<T> setValues(Iterator<T> values) {
      this.values = values;
      return this;
    }
  }
}
