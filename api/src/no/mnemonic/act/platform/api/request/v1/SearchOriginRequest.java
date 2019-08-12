package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.Min;

public class SearchOriginRequest implements ValidatingRequest {

  private Boolean includeDeleted;
  @Min(0)
  private Integer limit;

  public Boolean getIncludeDeleted() {
    return includeDeleted;
  }

  public SearchOriginRequest setIncludeDeleted(Boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public SearchOriginRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

}
