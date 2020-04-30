package no.mnemonic.act.platform.service.ti.tinkerpop;

/**
 * Immutable record that holds parameters that control the
 * traversal of graphs.
 */
public class TraverseParams {

  private final boolean includeRetracted;
  private final Long beforeTimestamp;
  private final Long afterTimestamp;

  private TraverseParams(boolean includeRetracted, Long beforeTimestamp, Long afterTimestamp) {
    this.includeRetracted = includeRetracted;
    this.beforeTimestamp = beforeTimestamp;
    this.afterTimestamp = afterTimestamp;
  }

  public boolean isIncludeRetracted() {
    return includeRetracted;
  }

  public Long getBeforeTimestamp() {
    return beforeTimestamp;
  }

  public Long getAfterTimestamp() {
    return afterTimestamp;
  }

  public static Builder builder() { return new Builder(); }

  public static class Builder {

    private boolean includeRetracted = false;
    private Long beforeTimestamp;
    private Long afterTimestamp;

    private Builder() {}

    public TraverseParams build() {
      return new TraverseParams(includeRetracted, beforeTimestamp, afterTimestamp);
    }

    public Builder setIncludeRetracted(boolean includeRetracted) {
      this.includeRetracted = includeRetracted;
      return this;
    }

    public Builder setBeforeTimestamp(Long beforeTimestamp) {
      this.beforeTimestamp = beforeTimestamp;
      return this;
    }

    public Builder setAfterTimestamp(Long afterTimestamp) {
      this.afterTimestamp = afterTimestamp;
      return this;
    }
  }
}
