package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.validation.constraints.Min;
import java.util.Set;

@ApiModel(description = "Search for Facts.")
public class SearchFactRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Only return Facts matching a keyword query")
  private String keywords;
  @ApiModelProperty(value = "Only return Facts with Objects having a specific ObjectType")
  private Set<String> objectType;
  @ApiModelProperty(value = "Only return Facts having a specific FactType")
  private Set<String> factType;
  @ApiModelProperty(value = "Only return Facts with Objects matching a specific value")
  private Set<String> objectValue;
  @ApiModelProperty(value = "Only return Facts matching a specific value")
  private Set<String> factValue;
  @ApiModelProperty(value = "Only return Facts belonging to a specific Organization")
  private Set<String> organization;
  @ApiModelProperty(value = "Only return Facts coming from a specific Source")
  private Set<String> source;
  @ApiModelProperty(value = "Include retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  @ApiModelProperty(value = "Only return Facts added before a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only return Facts added after a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
  @ApiModelProperty(value = "Limit the number of returned Facts (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;
  // TODO: Add minConfidence/maxConfidence once confidence level is defined.

  public String getKeywords() {
    return keywords;
  }

  public SearchFactRequest setKeywords(String keywords) {
    this.keywords = keywords;
    return this;
  }

  public Set<String> getObjectType() {
    return objectType;
  }

  public SearchFactRequest setObjectType(Set<String> objectType) {
    this.objectType = objectType;
    return this;
  }

  public SearchFactRequest addObjectType(String objectType) {
    this.objectType = SetUtils.addToSet(this.objectType, objectType);
    return this;
  }

  public Set<String> getFactType() {
    return factType;
  }

  public SearchFactRequest setFactType(Set<String> factType) {
    this.factType = factType;
    return this;
  }

  public SearchFactRequest addFactType(String factType) {
    this.factType = SetUtils.addToSet(this.factType, factType);
    return this;
  }

  public Set<String> getObjectValue() {
    return objectValue;
  }

  public SearchFactRequest setObjectValue(Set<String> objectValue) {
    this.objectValue = objectValue;
    return this;
  }

  public SearchFactRequest addObjectValue(String objectValue) {
    this.objectValue = SetUtils.addToSet(this.objectValue, objectValue);
    return this;
  }

  public Set<String> getFactValue() {
    return factValue;
  }

  public SearchFactRequest setFactValue(Set<String> factValue) {
    this.factValue = factValue;
    return this;
  }

  public SearchFactRequest addFactValue(String factValue) {
    this.factValue = SetUtils.addToSet(this.factValue, factValue);
    return this;
  }

  public Set<String> getOrganization() {
    return organization;
  }

  public SearchFactRequest setOrganization(Set<String> organization) {
    this.organization = organization;
    return this;
  }

  public SearchFactRequest addOrganization(String organization) {
    this.organization = SetUtils.addToSet(this.organization, organization);
    return this;
  }

  public Set<String> getSource() {
    return source;
  }

  public SearchFactRequest setSource(Set<String> source) {
    this.source = source;
    return this;
  }

  public SearchFactRequest addSource(String source) {
    this.source = SetUtils.addToSet(this.source, source);
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public SearchFactRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public SearchFactRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public SearchFactRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public SearchFactRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

}
