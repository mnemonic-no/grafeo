package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Set;

@ApiModel(description = "Request for traversing an Object/Fact graph")
public class TraverseGraphRequest implements TimeFieldSearchRequest, ValidatingRequest {

  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()", required = true)
  @NotBlank
  private String query;
  @ApiModelProperty(value = "Traverse retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  // Annotations are specified on the TimeFieldSearchRequest interface.
  private Long startTimestamp;
  private Long endTimestamp;
  private TimeMatchStrategy timeMatchStrategy;
  private Set<TimeFieldStrategy> timeFieldStrategy;
  @ApiModelProperty(value = "Limit the result size (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;

  public String getQuery() {
    return query;
  }

  public TraverseGraphRequest setQuery(String query) {
    this.query = query;
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public TraverseGraphRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  @Override
  public Long getStartTimestamp() {
    return startTimestamp;
  }

  public TraverseGraphRequest setStartTimestamp(Long timestamp) {
    this.startTimestamp = timestamp;
    return this;
  }

  @Override
  public Long getEndTimestamp() {
    return endTimestamp;
  }

  public TraverseGraphRequest setEndTimestamp(Long timestamp) {
    this.endTimestamp = timestamp;
    return this;
  }

  @Override
  public TimeMatchStrategy getTimeMatchStrategy() {
    return timeMatchStrategy;
  }

  public TraverseGraphRequest setTimeMatchStrategy(TimeMatchStrategy strategy) {
    this.timeMatchStrategy = strategy;
    return this;
  }

  @Override
  public Set<TimeFieldStrategy> getTimeFieldStrategy() {
    return timeFieldStrategy;
  }

  public TraverseGraphRequest setTimeFieldStrategy(Set<TimeFieldStrategy> strategy) {
    this.timeFieldStrategy = ObjectUtils.ifNotNull(strategy, SetUtils::set);
    return this;
  }

  public TraverseGraphRequest addTimeFieldStrategy(TimeFieldStrategy strategy) {
    this.timeFieldStrategy = SetUtils.addToSet(this.timeFieldStrategy, strategy);
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public TraverseGraphRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }
}
