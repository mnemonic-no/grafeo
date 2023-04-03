package no.mnemonic.services.grafeo.service.implementation.tinkerpop;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;

/**
 * Immutable record that holds parameters that control the traversal of graphs.
 */
public class TraverseParams {

  // This criteria will always be applied to searches during graph traversal (can be extended by additional parameters).
  private final FactSearchCriteria baseSearchCriteria;
  private final boolean includeRetracted;
  private final int limit;

  private TraverseParams(FactSearchCriteria baseSearchCriteria,
                         Boolean includeRetracted,
                         Integer limit) {
    this.baseSearchCriteria = ObjectUtils.notNull(baseSearchCriteria, "'baseSearchCriteria' is null!");
    this.includeRetracted = ObjectUtils.ifNull(includeRetracted, false);
    this.limit = ObjectUtils.ifNull(limit, 25);
  }

  public FactSearchCriteria getBaseSearchCriteria() {
    return baseSearchCriteria;
  }

  public Boolean isIncludeRetracted() {
    return includeRetracted;
  }

  public int getLimit() {
    return limit;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private FactSearchCriteria baseSearchCriteria;
    private Boolean includeRetracted;
    private Integer limit;

    private Builder() {
    }

    public TraverseParams build() {
      return new TraverseParams(baseSearchCriteria, includeRetracted, limit);
    }

    public Builder setBaseSearchCriteria(FactSearchCriteria baseSearchCriteria) {
      this.baseSearchCriteria = baseSearchCriteria;
      return this;
    }

    public Builder setIncludeRetracted(Boolean includeRetracted) {
      this.includeRetracted = includeRetracted;
      return this;
    }

    public Builder setLimit(Integer limit) {
      this.limit = limit;
      return this;
    }
  }
}
