package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.validation.constraints.Max;
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
  @ApiModelProperty(value = "Only return meta Facts coming from a specific Origin")
  private Set<String> origin;
  @ApiModelProperty(value = "Only return meta Facts where the field specified by 'dimension' is above a specific value (value between 0.0 and 1.0)")
  @Min(0)
  @Max(1)
  private Float minimum;
  @ApiModelProperty(value = "Only return meta Facts where the field specified by 'dimension' is below a specific value (value between 0.0 and 1.0)")
  @Min(0)
  @Max(1)
  private Float maximum;
  @ApiModelProperty(value = "Specify the field used for minimum/maximum filters (default 'certainty')")
  private Dimension dimension;
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
    this.factType = ObjectUtils.ifNotNull(factType, SetUtils::set);
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
    this.factValue = ObjectUtils.ifNotNull(factValue, SetUtils::set);
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
    this.organization = ObjectUtils.ifNotNull(organization, SetUtils::set);
    return this;
  }

  public SearchMetaFactsRequest addOrganization(String organization) {
    this.organization = SetUtils.addToSet(this.organization, organization);
    return this;
  }

  public Set<String> getOrigin() {
    return origin;
  }

  public SearchMetaFactsRequest setOrigin(Set<String> origin) {
    this.origin = ObjectUtils.ifNotNull(origin, SetUtils::set);
    return this;
  }

  public SearchMetaFactsRequest addOrigin(String origin) {
    this.origin = SetUtils.addToSet(this.origin, origin);
    return this;
  }

  public Float getMinimum() {
    return minimum;
  }

  public SearchMetaFactsRequest setMinimum(Float minimum) {
    this.minimum = minimum;
    return this;
  }

  public Float getMaximum() {
    return maximum;
  }

  public SearchMetaFactsRequest setMaximum(Float maximum) {
    this.maximum = maximum;
    return this;
  }

  public Dimension getDimension() {
    return dimension;
  }

  public SearchMetaFactsRequest setDimension(Dimension dimension) {
    this.dimension = dimension;
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
