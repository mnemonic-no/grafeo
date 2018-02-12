package no.mnemonic.act.platform.api.service.v1;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * The ResultSet is a general container holding the values returned from a service method. When a service method
 * produces multiple results, these results are transported to the REST layer wrapped inside a ResultSet.
 *
 * @param <T> Type of result values
 */
public class ResultSet<T> {

  private final int limit;
  private final int count;
  private final Collection<T> values;

  private ResultSet(int limit, int count, Collection<T> values) {
    this.limit = limit;
    this.count = count;
    this.values = ObjectUtils.ifNotNull(values, Collections::unmodifiableCollection, Collections.emptySet());
  }

  public int getLimit() {
    return limit;
  }

  public int getCount() {
    return count;
  }

  public Collection<T> getValues() {
    return values;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {
    private int limit;
    private int count;
    private Collection<T> values;

    private Builder() {
    }

    public ResultSet<T> build() {
      return new ResultSet<>(limit, count, values);
    }

    public Builder<T> setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder<T> setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder<T> setValues(Collection<T> values) {
      this.values = values;
      return this;
    }
  }

}
