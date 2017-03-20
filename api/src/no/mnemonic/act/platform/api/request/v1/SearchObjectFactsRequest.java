package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;

@ApiModel(description = "Search for Facts bound to a specific Object.")
public class SearchObjectFactsRequest implements ValidatingRequest {

  // Either objectID or objectType + objectValue must be set.
  @ApiModelProperty(hidden = true)
  private UUID objectID;
  @ApiModelProperty(hidden = true)
  private String objectType;
  @ApiModelProperty(hidden = true)
  private String objectValue;
  // Restrict returned Facts bound to one Object.
  @ApiModelProperty(value = "Only return Facts with a specific FactType")
  private Set<String> factType;
  @ApiModelProperty(value = "Only return Facts matching a specific value")
  private Set<String> factValue;
  @ApiModelProperty(value = "Only return Facts coming from a specific Source")
  private Set<String> source;
  @ApiModelProperty(value = "Include retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  @ApiModelProperty(value = "Only return Facts added before a specific timestamp", example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only return Facts added after a specific timestamp", example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
  @ApiModelProperty(value = "Limit the number of returned Facts (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;
  // TODO: Add minConfidence/maxConfidence once confidence level is defined.

  public UUID getObjectID() {
    return objectID;
  }

  public SearchObjectFactsRequest setObjectID(UUID objectID) {
    this.objectID = objectID;
    return this;
  }

  public String getObjectType() {
    return objectType;
  }

  public SearchObjectFactsRequest setObjectType(String objectType) {
    this.objectType = objectType;
    return this;
  }

  public String getObjectValue() {
    return objectValue;
  }

  public SearchObjectFactsRequest setObjectValue(String objectValue) {
    this.objectValue = objectValue;
    return this;
  }

  public Set<String> getFactType() {
    return factType;
  }

  public SearchObjectFactsRequest setFactType(Set<String> factType) {
    this.factType = factType;
    return this;
  }

  public SearchObjectFactsRequest addFactType(String factType) {
    this.factType = SetUtils.addToSet(this.factType, factType);
    return this;
  }

  public Set<String> getFactValue() {
    return factValue;
  }

  public SearchObjectFactsRequest setFactValue(Set<String> factValue) {
    this.factValue = factValue;
    return this;
  }

  public SearchObjectFactsRequest addFactValue(String factValue) {
    this.factValue = SetUtils.addToSet(this.factValue, factValue);
    return this;
  }

  public Set<String> getSource() {
    return source;
  }

  public SearchObjectFactsRequest setSource(Set<String> source) {
    this.source = source;
    return this;
  }

  public SearchObjectFactsRequest addSource(String source) {
    this.source = SetUtils.addToSet(this.source, source);
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public SearchObjectFactsRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public SearchObjectFactsRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public SearchObjectFactsRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public SearchObjectFactsRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

}
