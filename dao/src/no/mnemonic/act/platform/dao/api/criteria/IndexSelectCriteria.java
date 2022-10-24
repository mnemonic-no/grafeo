package no.mnemonic.act.platform.dao.api.criteria;

/**
 * Criteria to decide which indices in ElasticSearch will be queried.
 */
public class IndexSelectCriteria {

  private final long indexStartTimestamp;
  private final long indexEndTimestamp;

  private IndexSelectCriteria(long indexStartTimestamp, long indexEndTimestamp) {
    if (indexEndTimestamp < indexStartTimestamp)
      throw new IllegalArgumentException("'indexEndTimestamp' cannot be before 'indexStartTimestamp'!");

    this.indexStartTimestamp = indexStartTimestamp;
    this.indexEndTimestamp = indexEndTimestamp;
  }

  /**
   * Query daily indices newer than this timestamp.
   *
   * @return Start of indices to query
   */
  public long getIndexStartTimestamp() {
    return indexStartTimestamp;
  }

  /**
   * Query daily indices older than this timestamp.
   *
   * @return End of indices to query
   */
  public long getIndexEndTimestamp() {
    return indexEndTimestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long indexStartTimestamp;
    private long indexEndTimestamp;

    private Builder() {
    }

    public IndexSelectCriteria build() {
      return new IndexSelectCriteria(indexStartTimestamp, indexEndTimestamp);
    }

    public Builder setIndexStartTimestamp(long indexStartTimestamp) {
      this.indexStartTimestamp = indexStartTimestamp;
      return this;
    }

    public Builder setIndexEndTimestamp(long indexEndTimestamp) {
      this.indexEndTimestamp = indexEndTimestamp;
      return this;
    }
  }
}
