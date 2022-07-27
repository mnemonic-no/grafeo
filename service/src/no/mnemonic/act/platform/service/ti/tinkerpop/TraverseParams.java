package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.commons.utilities.ObjectUtils;

/**
 * Immutable record that holds parameters that control the
 * traversal of graphs.
 */
public class TraverseParams {

  private final AccessControlCriteria accessControlCriteria;
  private final IndexSelectCriteria indexSelectCriteria;
  private final boolean includeRetracted;
  private final Long beforeTimestamp;
  private final Long afterTimestamp;
  private final int limit;

  private TraverseParams(AccessControlCriteria accessControlCriteria,
                         IndexSelectCriteria indexSelectCriteria,
                         Boolean includeRetracted,
                         Long beforeTimestamp,
                         Long afterTimestamp,
                         Integer limit) {
    this.accessControlCriteria = accessControlCriteria;
    this.indexSelectCriteria = indexSelectCriteria;
    this.includeRetracted = ObjectUtils.ifNull(includeRetracted, false);
    this.beforeTimestamp = beforeTimestamp;
    this.afterTimestamp = afterTimestamp;
    this.limit = ObjectUtils.ifNull(limit, 25);
  }

  public AccessControlCriteria getAccessControlCriteria() {
    return accessControlCriteria;
  }

  public IndexSelectCriteria getIndexSelectCriteria() {
    return indexSelectCriteria;
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private AccessControlCriteria accessControlCriteria;
    private IndexSelectCriteria indexSelectCriteria;
    private Boolean includeRetracted;
    private Long beforeTimestamp;
    private Long afterTimestamp;
    private Integer limit;

    private Builder() {
    }

    public TraverseParams build() {
      return new TraverseParams(accessControlCriteria, indexSelectCriteria, includeRetracted, beforeTimestamp, afterTimestamp, limit);
    }

    public Builder setAccessControlCriteria(AccessControlCriteria accessControlCriteria) {
      this.accessControlCriteria = accessControlCriteria;
      return this;
    }

    public Builder setIndexSelectCriteria(IndexSelectCriteria indexSelectCriteria) {
      this.indexSelectCriteria = indexSelectCriteria;
      return this;
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
