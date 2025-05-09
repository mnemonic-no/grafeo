package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatDeserializer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Search for Facts bound to a specific Object.")
public class SearchObjectFactsRequest implements TimeFieldSearchRequest, ValidatingRequest {

  // Either objectID or objectType + objectValue must be set.
  @Schema(hidden = true)
  private UUID objectID;
  @Schema(hidden = true)
  private String objectType;
  @Schema(hidden = true)
  private String objectValue;
  // Restrict returned Facts bound to one Object.
  @Schema(description = "Only return Facts matching a keyword query")
  private String keywords;
  @Schema(description = "Only return Facts with a specific FactType")
  private Set<String> factType;
  @Schema(description = "Only return Facts matching a specific value")
  private Set<String> factValue;
  @Schema(description = "Only return Facts belonging to a specific Organization")
  private Set<String> organization;
  @Schema(description = "Only return Facts coming from a specific Origin")
  private Set<String> origin;
  @Schema(description = "Only return Facts where the field specified by 'dimension' is above a specific value (value between 0.0 and 1.0)")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float minimum;
  @Schema(description = "Only return Facts where the field specified by 'dimension' is below a specific value (value between 0.0 and 1.0)")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float maximum;
  @Schema(description = "Specify the field used for minimum/maximum filters (default 'certainty')")
  private Dimension dimension;
  @Schema(description = "Include retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  // Annotations are specified on the TimeFieldSearchRequest interface.
  private Long startTimestamp;
  private Long endTimestamp;
  private TimeMatchStrategy timeMatchStrategy;
  private Set<TimeFieldStrategy> timeFieldStrategy;
  @Schema(description = "Limit the number of returned Facts (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;

  public String getKeywords() {
    return keywords;
  }

  public SearchObjectFactsRequest setKeywords(String keywords) {
    this.keywords = keywords;
    return this;
  }

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
    this.factType = ObjectUtils.ifNotNull(factType, SetUtils::set);
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
    this.factValue = ObjectUtils.ifNotNull(factValue, SetUtils::set);
    return this;
  }

  public SearchObjectFactsRequest addFactValue(String factValue) {
    this.factValue = SetUtils.addToSet(this.factValue, factValue);
    return this;
  }

  public Set<String> getOrganization() {
    return organization;
  }

  public SearchObjectFactsRequest setOrganization(Set<String> organization) {
    this.organization = ObjectUtils.ifNotNull(organization, SetUtils::set);
    return this;
  }

  public SearchObjectFactsRequest addOrganization(String organization) {
    this.organization = SetUtils.addToSet(this.organization, organization);
    return this;
  }

  public Set<String> getOrigin() {
    return origin;
  }

  public SearchObjectFactsRequest setOrigin(Set<String> origin) {
    this.origin = ObjectUtils.ifNotNull(origin, SetUtils::set);
    return this;
  }

  public SearchObjectFactsRequest addOrigin(String origin) {
    this.origin = SetUtils.addToSet(this.origin, origin);
    return this;
  }

  public Float getMinimum() {
    return minimum;
  }

  public SearchObjectFactsRequest setMinimum(Float minimum) {
    this.minimum = minimum;
    return this;
  }

  public Float getMaximum() {
    return maximum;
  }

  public SearchObjectFactsRequest setMaximum(Float maximum) {
    this.maximum = maximum;
    return this;
  }

  public Dimension getDimension() {
    return dimension;
  }

  public SearchObjectFactsRequest setDimension(Dimension dimension) {
    this.dimension = dimension;
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public SearchObjectFactsRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  @Override
  public Long getStartTimestamp() {
    return startTimestamp;
  }

  public SearchObjectFactsRequest setStartTimestamp(Long timestamp) {
    this.startTimestamp = timestamp;
    return this;
  }

  @Override
  public Long getEndTimestamp() {
    return endTimestamp;
  }

  public SearchObjectFactsRequest setEndTimestamp(Long timestamp) {
    this.endTimestamp = timestamp;
    return this;
  }

  @Override
  public TimeMatchStrategy getTimeMatchStrategy() {
    return timeMatchStrategy;
  }

  public SearchObjectFactsRequest setTimeMatchStrategy(TimeMatchStrategy strategy) {
    this.timeMatchStrategy = strategy;
    return this;
  }

  @Override
  public Set<TimeFieldStrategy> getTimeFieldStrategy() {
    return timeFieldStrategy;
  }

  public SearchObjectFactsRequest setTimeFieldStrategy(Set<TimeFieldStrategy> strategy) {
    this.timeFieldStrategy = ObjectUtils.ifNotNull(strategy, SetUtils::set);
    return this;
  }

  public SearchObjectFactsRequest addTimeFieldStrategy(TimeFieldStrategy strategy) {
    this.timeFieldStrategy = SetUtils.addToSet(this.timeFieldStrategy, strategy);
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
