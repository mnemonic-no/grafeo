package no.mnemonic.services.grafeo.api.service.v1;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.api.ResultSet;

import java.util.Collections;
import java.util.Iterator;

/**
 * The {@link ResultSet} is a general container holding the values returned from a service method. When a service method
 * produces multiple results, these results are transported to the REST layer wrapped inside a {@link ResultSet}.
 * <p>
 * {@link StreamingResultSet} is a {@link ResultSet} implementation which allows streaming of results to the REST layer.
 *
 * @param <T> Type of result values
 */
public class StreamingResultSet<T> implements ResultSet<T> {

  private final int limit;
  private final int count;
  private final int offset;
  private final Iterator<T> values;

  private StreamingResultSet(int limit, int count, int offset, Iterator<T> values) {
    this.limit = limit;
    this.count = count;
    this.offset = offset;
    this.values = ObjectUtils.ifNull(values, Collections.emptyIterator());
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public int getCount() {
    return count;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public Iterator<T> iterator() {
    return values;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {
    private int limit;
    private int count;
    private int offset;
    private Iterator<T> values;

    private Builder() {
    }

    public StreamingResultSet<T> build() {
      return new StreamingResultSet<>(limit, count, offset, values);
    }

    public Builder<T> setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder<T> setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder<T> setOffset(int offset) {
      this.offset = offset;
      return this;
    }

    public Builder<T> setValues(Iterator<T> values) {
      this.values = values;
      return this;
    }

    public Builder<T> setValues(Iterable<T> values) {
      this.values = ObjectUtils.ifNotNull(values, Iterable::iterator);
      return this;
    }
  }

}
