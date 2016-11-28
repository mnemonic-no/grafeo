package no.mnemonic.act.platform.api.model.v1;

public class ObjectFactsStatistic {

  private final FactType.Info type;
  private final int count;
  private final String lastAddedTimestamp;
  private final String lastSeenTimestamp;
  // TODO: Add minConfidenceLevel/maxConfidenceLevel once confidence levels are defined.

  private ObjectFactsStatistic(FactType.Info type, int count, String lastAddedTimestamp, String lastSeenTimestamp) {
    this.type = type;
    this.count = count;
    this.lastAddedTimestamp = lastAddedTimestamp;
    this.lastSeenTimestamp = lastSeenTimestamp;
  }

  public FactType.Info getType() {
    return type;
  }

  public int getCount() {
    return count;
  }

  public String getLastAddedTimestamp() {
    return lastAddedTimestamp;
  }

  public String getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactType.Info type;
    private int count;
    private String lastAddedTimestamp;
    private String lastSeenTimestamp;

    private Builder() {
    }

    public ObjectFactsStatistic build() {
      return new ObjectFactsStatistic(type, count, lastAddedTimestamp, lastSeenTimestamp);
    }

    public Builder setType(FactType.Info type) {
      this.type = type;
      return this;
    }

    public Builder setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder setLastAddedTimestamp(String lastAddedTimestamp) {
      this.lastAddedTimestamp = lastAddedTimestamp;
      return this;
    }

    public Builder setLastSeenTimestamp(String lastSeenTimestamp) {
      this.lastSeenTimestamp = lastSeenTimestamp;
      return this;
    }
  }

}
