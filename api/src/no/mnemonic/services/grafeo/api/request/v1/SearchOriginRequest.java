package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import jakarta.validation.constraints.Min;
import java.util.Set;

@Schema(description = "Search for Origins.")
public class SearchOriginRequest implements ValidatingRequest {

  public enum Type {
    Group, User
  }

  @Schema(description = "Only return Origins having a specific type")
  private Set<Type> type;
  @Schema(description = "Include deleted Origins (default false)", example = "false")
  private Boolean includeDeleted;
  @Schema(description = "Limit the number of returned Origins (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;

  public Set<Type> getType() {
    return type;
  }

  public SearchOriginRequest setType(Set<Type> type) {
    this.type = ObjectUtils.ifNotNull(type, SetUtils::set);
    return this;
  }

  public SearchOriginRequest addType(Type type) {
    this.type = SetUtils.addToSet(this.type, type);
    return this;
  }

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
