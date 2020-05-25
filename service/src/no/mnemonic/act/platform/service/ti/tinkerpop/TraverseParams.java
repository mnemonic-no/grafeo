package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.commons.utilities.ObjectUtils;

/**
 * Immutable record that holds parameters that control the
 * traversal of graphs.
 */
public class TraverseParams {

  private final boolean includeRetracted;
  private final Long beforeTimestamp;
  private final Long afterTimestamp;
  private final int limit;

  private TraverseParams(Boolean includeRetracted, Long beforeTimestamp, Long afterTimestamp, Integer limit) {
    this.includeRetracted = ObjectUtils.ifNull(includeRetracted, false);
    this.beforeTimestamp = beforeTimestamp;
    this.afterTimestamp = afterTimestamp;
    this.limit = ObjectUtils.ifNull(limit, 25);
  }

  public Boolean isIncludeRetracted() {
    return includeRetracted;
  }

  public Long getBeforeTimestamp() {
    return beforeTimestamp;
  }

  public Long getAfterTimestamp() {
    return afterTimestamp;
  }

  public int getLimit() {
    return limit;
  }

  public static Builder builder() { return new Builder(); }

  public static class Builder {

    private Boolean includeRetracted;
    private Long beforeTimestamp;
    private Long afterTimestamp;
    private Integer limit;

    private Builder() {}

    public TraverseParams build() {
      return new TraverseParams(includeRetracted, beforeTimestamp, afterTimestamp, limit);
    }

    public Builder setIncludeRetracted(Boolean includeRetracted) {
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

    public Builder setLimit(Integer limit) {
      this.limit = limit;
      return this;
    }
  }
}
