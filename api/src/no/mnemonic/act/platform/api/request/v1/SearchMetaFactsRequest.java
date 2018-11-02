package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;

@ApiModel(description = "Search for meta Facts bound to a specific Fact.")
public class SearchMetaFactsRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @ApiModelProperty(value = "Only return meta Facts matching a keyword query")
  private String keywords;
  @ApiModelProperty(value = "Only return meta Facts with a specific FactType")
  private Set<String> factType;
  @ApiModelProperty(value = "Only return meta Facts matching a specific value")
  private Set<String> factValue;
  @ApiModelProperty(value = "Only return meta Facts belonging to a specific Organization")
  private Set<String> organization;
  @ApiModelProperty(value = "Only return meta Facts coming from a specific Source")
  private Set<String> source;
  @ApiModelProperty(value = "Include retracted meta Facts (default false)", example = "false")
  private Boolean includeRetracted;
  @ApiModelProperty(value = "Only return meta Facts added before a specific timestamp", example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only return meta Facts added after a specific timestamp", example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
  @ApiModelProperty(value = "Limit the number of returned meta Facts (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;
  // TODO: Add minConfidence/maxConfidence once confidence level is defined.

  public UUID getFact() {
    return fact;
  }

  public SearchMetaFactsRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public String getKeywords() {
    return keywords;
  }

  public SearchMetaFactsRequest setKeywords(String keywords) {
    this.keywords = keywords;
    return this;
  }

  public Set<String> getFactType() {
    return factType;
  }

  public SearchMetaFactsRequest setFactType(Set<String> factType) {
    this.factType = factType;
    return this;
  }

  public SearchMetaFactsRequest addFactType(String factType) {
    this.factType = SetUtils.addToSet(this.factType, factType);
    return this;
  }

  public Set<String> getFactValue() {
    return factValue;
  }

  public SearchMetaFactsRequest setFactValue(Set<String> factValue) {
    this.factValue = factValue;
    return this;
  }

  public SearchMetaFactsRequest addFactValue(String factValue) {
    this.factValue = SetUtils.addToSet(this.factValue, factValue);
    return this;
  }

  public Set<String> getOrganization() {
    return organization;
  }

  public SearchMetaFactsRequest setOrganization(Set<String> organization) {
    this.organization = organization;
    return this;
  }

  public SearchMetaFactsRequest addOrganization(String organization) {
    this.organization = SetUtils.addToSet(this.organization, organization);
    return this;
  }

  public Set<String> getSource() {
    return source;
  }

  public SearchMetaFactsRequest setSource(Set<String> source) {
    this.source = source;
    return this;
  }

  public SearchMetaFactsRequest addSource(String source) {
    this.source = SetUtils.addToSet(this.source, source);
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public SearchMetaFactsRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public SearchMetaFactsRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public SearchMetaFactsRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public SearchMetaFactsRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

}
