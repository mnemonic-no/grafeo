package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatDeserializer;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Search for meta Facts bound to a specific Fact.")
public class SearchMetaFactsRequest implements TimeFieldSearchRequest, ValidatingRequest {

  @Schema(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @Schema(description = "Only return meta Facts matching a keyword query")
  private String keywords;
  @Schema(description = "Only return meta Facts with a specific FactType")
  private Set<String> factType;
  @Schema(description = "Only return meta Facts matching a specific value")
  private Set<String> factValue;
  @Schema(description = "Only return meta Facts belonging to a specific Organization")
  private Set<String> organization;
  @Schema(description = "Only return meta Facts coming from a specific Origin")
  private Set<String> origin;
  @Schema(description = "Only return meta Facts where the field specified by 'dimension' is above a specific value (value between 0.0 and 1.0)")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float minimum;
  @Schema(description = "Only return meta Facts where the field specified by 'dimension' is below a specific value (value between 0.0 and 1.0)")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float maximum;
  @Schema(description = "Specify the field used for minimum/maximum filters (default 'certainty')")
  private Dimension dimension;
  @Schema(description = "Include retracted meta Facts (default false)", example = "false")
  private Boolean includeRetracted;
  // Annotations are specified on the TimeFieldSearchRequest interface.
  private Long startTimestamp;
  private Long endTimestamp;
  private TimeMatchStrategy timeMatchStrategy;
  private Set<TimeFieldStrategy> timeFieldStrategy;
  @Schema(description = "Limit the number of returned meta Facts (default 25, 0 means all)", example = "25")
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

  @Override
  public Long getStartTimestamp() {
    return startTimestamp;
  }

  public SearchMetaFactsRequest setStartTimestamp(Long timestamp) {
    this.startTimestamp = timestamp;
    return this;
  }

  @Override
  public Long getEndTimestamp() {
    return endTimestamp;
  }

  public SearchMetaFactsRequest setEndTimestamp(Long timestamp) {
    this.endTimestamp = timestamp;
    return this;
  }

  @Override
  public TimeMatchStrategy getTimeMatchStrategy() {
    return timeMatchStrategy;
  }

  public SearchMetaFactsRequest setTimeMatchStrategy(TimeMatchStrategy strategy) {
    this.timeMatchStrategy = strategy;
    return this;
  }

  @Override
  public Set<TimeFieldStrategy> getTimeFieldStrategy() {
    return timeFieldStrategy;
  }

  public SearchMetaFactsRequest setTimeFieldStrategy(Set<TimeFieldStrategy> strategy) {
    this.timeFieldStrategy = ObjectUtils.ifNotNull(strategy, SetUtils::set);
    return this;
  }

  public SearchMetaFactsRequest addTimeFieldStrategy(TimeFieldStrategy strategy) {
    this.timeFieldStrategy = SetUtils.addToSet(this.timeFieldStrategy, strategy);
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
