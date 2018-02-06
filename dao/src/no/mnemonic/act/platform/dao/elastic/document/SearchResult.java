package no.mnemonic.act.platform.dao.elastic.document;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.Collections;
import java.util.List;

/**
 * Container holding the results of a search in ElasticSearch, i.e. the matching documents.
 *
 * @param <T> Type of result values
 */
public class SearchResult<T extends ElasticDocument> {

  private final int limit;
  private final int count;
  private final List<T> values;

  private SearchResult(int limit, int count, List<T> values) {
    this.limit = limit;
    this.count = count;
    this.values = ObjectUtils.ifNotNull(values, Collections::unmodifiableList, Collections.emptyList());
  }

  /**
   * Returns the maximum possible number of results. The actual number of results might be smaller.
   *
   * @return Maximum number of results
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Returns the total number of indexed documents matching a given search criteria.
   *
   * @return Total number of matching documents
   */
  public int getCount() {
    return count;
  }

  /**
   * Returns the actual search results, but not more than specified by 'limit'.
   *
   * @return Search results
   */
  public List<T> getValues() {
    return values;
  }

  public static <T extends ElasticDocument> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T extends ElasticDocument> {
    private int limit;
    private int count;
    private List<T> values;

    private Builder() {
    }

    public SearchResult<T> build() {
      return new SearchResult<>(limit, count, values);
    }

    public Builder<T> setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder<T> setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder<T> setValues(List<T> values) {
      this.values = values;
      return this;
    }

    public Builder<T> addValue(T value) {
      this.values = ListUtils.addToList(this.values, value);
      return this;
    }
  }

}
