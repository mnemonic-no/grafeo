package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;

@ApiModel(description = "Search for Objects.")
public class SearchObjectRequest {

  @ApiModelProperty(value = "Shortcut which combines both 'objectType' and 'factType'")
  private Set<String> type;
  @ApiModelProperty(value = "Only return Objects with a specific ObjectType")
  private Set<String> objectType;
  @ApiModelProperty(value = "Only return Objects with Facts having a specific FactType")
  private Set<String> factType;
  @ApiModelProperty(value = "Shortcut which combines both 'objectValue' and 'factValue'")
  private Set<String> value;
  @ApiModelProperty(value = "Only return Objects matching a specific value")
  private Set<String> objectValue;
  @ApiModelProperty(value = "Only return Objects with Facts matching a specific value")
  private Set<String> factValue;
  @ApiModelProperty(value = "Only return Objects with Facts coming from a specific Source")
  private Set<String> source;
  @ApiModelProperty(value = "Only return Objects with Facts added before a specific timestamp",
          example = "2016-09-28T21:26:22", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only return Objects with Facts added after a specific timestamp",
          example = "2016-09-28T21:26:22", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
  @ApiModelProperty(value = "Limit the number of returned Objects (default 25, 0 means all)", example = "25")
  private Integer limit;
  // TODO: Add minConfidence/maxConfidence once confidence level is defined.

  public Set<String> getType() {
    return type;
  }

  public SearchObjectRequest setType(Set<String> type) {
    this.type = type;
    return this;
  }

  public SearchObjectRequest addType(String type) {
    this.type = SetUtils.addToSet(this.type, type);
    return this;
  }

  public Set<String> getObjectType() {
    return objectType;
  }

  public SearchObjectRequest setObjectType(Set<String> objectType) {
    this.objectType = objectType;
    return this;
  }

  public SearchObjectRequest addObjectType(String objectType) {
    this.objectType = SetUtils.addToSet(this.objectType, objectType);
    return this;
  }

  public Set<String> getFactType() {
    return factType;
  }

  public SearchObjectRequest setFactType(Set<String> factType) {
    this.factType = factType;
    return this;
  }

  public SearchObjectRequest addFactType(String factType) {
    this.factType = SetUtils.addToSet(this.factType, factType);
    return this;
  }

  public Set<String> getValue() {
    return value;
  }

  public SearchObjectRequest setValue(Set<String> value) {
    this.value = value;
    return this;
  }

  public SearchObjectRequest addValue(String value) {
    this.value = SetUtils.addToSet(this.value, value);
    return this;
  }

  public Set<String> getObjectValue() {
    return objectValue;
  }

  public SearchObjectRequest setObjectValue(Set<String> objectValue) {
    this.objectValue = objectValue;
    return this;
  }

  public SearchObjectRequest addObjectValue(String objectValue) {
    this.objectValue = SetUtils.addToSet(this.objectValue, objectValue);
    return this;
  }

  public Set<String> getFactValue() {
    return factValue;
  }

  public SearchObjectRequest setFactValue(Set<String> factValue) {
    this.factValue = factValue;
    return this;
  }

  public SearchObjectRequest addFactValue(String factValue) {
    this.factValue = SetUtils.addToSet(this.factValue, factValue);
    return this;
  }

  public Set<String> getSource() {
    return source;
  }

  public SearchObjectRequest setSource(Set<String> source) {
    this.source = source;
    return this;
  }

  public SearchObjectRequest addSource(String source) {
    this.source = SetUtils.addToSet(this.source, source);
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public SearchObjectRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public SearchObjectRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public SearchObjectRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

}
