package no.mnemonic.act.platform.dao.api.criteria;

/**
 * Criteria to decide which indices in ElasticSearch will be queried.
 */
public class IndexSelectCriteria {

  private final long indexStartTimestamp;
  private final long indexEndTimestamp;
  private final boolean useLegacyIndex;

  private IndexSelectCriteria(long indexStartTimestamp, long indexEndTimestamp, boolean useLegacyIndex) {
    if (indexEndTimestamp < indexStartTimestamp)
      throw new IllegalArgumentException("'indexEndTimestamp' cannot be before 'indexStartTimestamp'!");

    this.indexStartTimestamp = indexStartTimestamp;
    this.indexEndTimestamp = indexEndTimestamp;
    this.useLegacyIndex = useLegacyIndex;
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

  /**
   * If true ignore indexStartTimestamp/indexEndTimestamp and query the legacy 'act' index,
   * otherwise query daily indices based on indexStartTimestamp/indexEndTimestamp.
   *
   * @return Whether to query daily indices or the legacy 'act' index.
   */
  public boolean isUseLegacyIndex() {
    return useLegacyIndex;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long indexStartTimestamp;
    private long indexEndTimestamp;
    private boolean useLegacyIndex;

    private Builder() {
    }

    public IndexSelectCriteria build() {
      return new IndexSelectCriteria(indexStartTimestamp, indexEndTimestamp, useLegacyIndex);
    }

    public Builder setIndexStartTimestamp(long indexStartTimestamp) {
      this.indexStartTimestamp = indexStartTimestamp;
      return this;
    }

    public Builder setIndexEndTimestamp(long indexEndTimestamp) {
      this.indexEndTimestamp = indexEndTimestamp;
      return this;
    }

    public Builder setUseLegacyIndex(boolean useLegacyIndex) {
      this.useLegacyIndex = useLegacyIndex;
      return this;
    }
  }
}
