package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.mnemonic.act.platform.api.json.TimestampSerializer;

public class ObjectFactsStatistic {

  private final FactType.Info type;
  private final int count;
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long lastAddedTimestamp;
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long lastSeenTimestamp;
  // TODO: Add minConfidenceLevel/maxConfidenceLevel once confidence levels are defined.

  private ObjectFactsStatistic(FactType.Info type, int count, Long lastAddedTimestamp, Long lastSeenTimestamp) {
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

  public Long getLastAddedTimestamp() {
    return lastAddedTimestamp;
  }

  public Long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactType.Info type;
    private int count;
    private Long lastAddedTimestamp;
    private Long lastSeenTimestamp;

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

    public Builder setLastAddedTimestamp(Long lastAddedTimestamp) {
      this.lastAddedTimestamp = lastAddedTimestamp;
      return this;
    }

    public Builder setLastSeenTimestamp(Long lastSeenTimestamp) {
      this.lastSeenTimestamp = lastSeenTimestamp;
      return this;
    }
  }

}
