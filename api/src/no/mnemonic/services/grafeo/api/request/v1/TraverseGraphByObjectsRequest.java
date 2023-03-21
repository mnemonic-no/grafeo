package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.utilities.json.TimestampDeserializer;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

@ApiModel(description = "Traverse the Object/Fact graph starting at a set of objects identified by either their id " +
        "or type/value tuple.")
public class TraverseGraphByObjectsRequest implements TimeFieldSearchRequest, ValidatingRequest {

  @ApiModelProperty(
          value = "Set of object identifiers. Takes Object UUID or Object identified by 'type/value')",
          example = "['123e4567-e89b-12d3-a456-426655440000', 'ThreatActor/Sofacy']",
          required = true)
  @NotEmpty
  private Set<String> objects;

  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()", required = true)
  @NotBlank
  private String query;
  @ApiModelProperty(value = "Traverse retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  @ApiModelProperty(value = "Deprecated: Use endTimestamp instead", example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Deprecated: Use startTimestamp instead", example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
  // Annotations are specified on the TimeFieldSearchRequest interface.
  private TimeMatchStrategy timeMatchStrategy;
  private Set<TimeFieldStrategy> timeFieldStrategy;
  @ApiModelProperty(value = "Limit the result size (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;

  public TraverseGraphByObjectsRequest setObjects(Set<String> objects) {
    this.objects = ObjectUtils.ifNotNull(objects, SetUtils::set);
    return this;
  }

  public TraverseGraphByObjectsRequest addObject(String object) {
    this.objects = SetUtils.addToSet(this.objects, object);
    return this;
  }

  public Set<String> getObjects() {
    return objects;
  }

  public String getQuery() {
    return query;
  }

  public TraverseGraphByObjectsRequest setQuery(String query) {
    this.query = query;
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public TraverseGraphByObjectsRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public TraverseGraphByObjectsRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public TraverseGraphByObjectsRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

  @Override
  public Long getStartTimestamp() {
    return getAfter();
  }

  public TraverseGraphByObjectsRequest setStartTimestamp(Long timestamp) {
    return setAfter(timestamp);
  }

  @Override
  public Long getEndTimestamp() {
    return getBefore();
  }

  public TraverseGraphByObjectsRequest setEndTimestamp(Long timestamp) {
    return setBefore(timestamp);
  }

  @Override
  public TimeMatchStrategy getTimeMatchStrategy() {
    return timeMatchStrategy;
  }

  public TraverseGraphByObjectsRequest setTimeMatchStrategy(TimeMatchStrategy strategy) {
    this.timeMatchStrategy = strategy;
    return this;
  }

  @Override
  public Set<TimeFieldStrategy> getTimeFieldStrategy() {
    return timeFieldStrategy;
  }

  public TraverseGraphByObjectsRequest setTimeFieldStrategy(Set<TimeFieldStrategy> strategy) {
    this.timeFieldStrategy = ObjectUtils.ifNotNull(strategy, SetUtils::set);
    return this;
  }

  public TraverseGraphByObjectsRequest addTimeFieldStrategy(TimeFieldStrategy strategy) {
    this.timeFieldStrategy = SetUtils.addToSet(this.timeFieldStrategy, strategy);
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public TraverseGraphByObjectsRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public static TraverseGraphByObjectsRequest from(TraverseGraphRequest request, String object) {
    return new TraverseGraphByObjectsRequest()
            .setQuery(request.getQuery())
            .setAfter(request.getAfter())
            .setBefore(request.getBefore())
            .setStartTimestamp(request.getStartTimestamp())
            .setEndTimestamp(request.getEndTimestamp())
            .setTimeMatchStrategy(request.getTimeMatchStrategy())
            .setTimeFieldStrategy(request.getTimeFieldStrategy())
            .setIncludeRetracted(request.getIncludeRetracted())
            .setLimit(request.getLimit())
            .addObject(object);
  }
}
