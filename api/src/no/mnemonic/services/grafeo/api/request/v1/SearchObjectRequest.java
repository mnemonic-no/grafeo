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

@Schema(description = "Search for Objects.")
public class SearchObjectRequest implements TimeFieldSearchRequest, ValidatingRequest {

  @Schema(description = "Only return Objects matching a keyword query")
  private String keywords;
  @Schema(description = "Only return Objects identified by their UUID")
  private Set<UUID> objectID;
  @Schema(description = "Only return Objects bound to specific Facts")
  private Set<UUID> factID;
  @Schema(description = "Only return Objects with a specific ObjectType")
  private Set<String> objectType;
  @Schema(description = "Only return Objects with Facts having a specific FactType")
  private Set<String> factType;
  @Schema(description = "Only return Objects matching a specific value")
  private Set<String> objectValue;
  @Schema(description = "Only return Objects with Facts matching a specific value")
  private Set<String> factValue;
  @Schema(description = "Only return Objects with Facts belonging to a specific Organization")
  private Set<String> organization;
  @Schema(description = "Only return Objects with Facts coming from a specific Origin")
  private Set<String> origin;
  @Schema(description = "Only return Objects with Facts where the field specified by 'dimension' is above a specific value (value between 0.0 and 1.0)")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float minimum;
  @Schema(description = "Only return Objects with Facts where the field specified by 'dimension' is below a specific value (value between 0.0 and 1.0)")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float maximum;
  @Schema(description = "Specify the field used for minimum/maximum filters (default 'certainty')")
  private Dimension dimension;
  // Annotations are specified on the TimeFieldSearchRequest interface.
  private Long startTimestamp;
  private Long endTimestamp;
  private TimeMatchStrategy timeMatchStrategy;
  private Set<TimeFieldStrategy> timeFieldStrategy;
  @Schema(description = "Only return Objects where the amount of Facts after filtering is above a specific value (inclusive)")
  @Min(0)
  private Integer minimumFactsCount;
  @Schema(description = "Only return Objects where the amount of Facts after filtering is below a specific value (inclusive)")
  @Min(0)
  private Integer maximumFactsCount;
  @Schema(description = "Limit the number of returned Objects (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;
  @Schema(description = "Specify whether Object statistics should be included in the response (omitted by default)")
  private boolean includeStatistics;

  public String getKeywords() {
    return keywords;
  }

  public Set<UUID> getObjectID() {
    return objectID;
  }

  public SearchObjectRequest setObjectID(Set<UUID> objectID) {
    this.objectID = ObjectUtils.ifNotNull(objectID, SetUtils::set);
    return this;
  }

  public SearchObjectRequest addObjectID(UUID objectID) {
    this.objectID = SetUtils.addToSet(this.objectID, objectID);
    return this;
  }

  public Set<UUID> getFactID() {
    return factID;
  }

  public SearchObjectRequest setFactID(Set<UUID> factID) {
    this.factID = ObjectUtils.ifNotNull(factID, SetUtils::set);
    return this;
  }

  public SearchObjectRequest addFactID(UUID factID) {
    this.factID = SetUtils.addToSet(this.factID, factID);
    return this;
  }

  public SearchObjectRequest setKeywords(String keywords) {
    this.keywords = keywords;
    return this;
  }

  public Set<String> getObjectType() {
    return objectType;
  }

  public SearchObjectRequest setObjectType(Set<String> objectType) {
    this.objectType = ObjectUtils.ifNotNull(objectType, SetUtils::set);
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
    this.factType = ObjectUtils.ifNotNull(factType, SetUtils::set);
    return this;
  }

  public SearchObjectRequest addFactType(String factType) {
    this.factType = SetUtils.addToSet(this.factType, factType);
    return this;
  }

  public Set<String> getObjectValue() {
    return objectValue;
  }

  public SearchObjectRequest setObjectValue(Set<String> objectValue) {
    this.objectValue = ObjectUtils.ifNotNull(objectValue, SetUtils::set);
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
    this.factValue = ObjectUtils.ifNotNull(factValue, SetUtils::set);
    return this;
  }

  public SearchObjectRequest addFactValue(String factValue) {
    this.factValue = SetUtils.addToSet(this.factValue, factValue);
    return this;
  }

  public Set<String> getOrganization() {
    return organization;
  }

  public SearchObjectRequest setOrganization(Set<String> organization) {
    this.organization = ObjectUtils.ifNotNull(organization, SetUtils::set);
    return this;
  }

  public SearchObjectRequest addOrganization(String organization) {
    this.organization = SetUtils.addToSet(this.organization, organization);
    return this;
  }

  public Set<String> getOrigin() {
    return origin;
  }

  public SearchObjectRequest setOrigin(Set<String> origin) {
    this.origin = ObjectUtils.ifNotNull(origin, SetUtils::set);
    return this;
  }

  public SearchObjectRequest addOrigin(String origin) {
    this.origin = SetUtils.addToSet(this.origin, origin);
    return this;
  }

  public Float getMinimum() {
    return minimum;
  }

  public SearchObjectRequest setMinimum(Float minimum) {
    this.minimum = minimum;
    return this;
  }

  public Float getMaximum() {
    return maximum;
  }

  public SearchObjectRequest setMaximum(Float maximum) {
    this.maximum = maximum;
    return this;
  }

  public Dimension getDimension() {
    return dimension;
  }

  public SearchObjectRequest setDimension(Dimension dimension) {
    this.dimension = dimension;
    return this;
  }

  @Override
  public Long getStartTimestamp() {
    return startTimestamp;
  }

  public SearchObjectRequest setStartTimestamp(Long timestamp) {
    this.startTimestamp = timestamp;
    return this;
  }

  @Override
  public Long getEndTimestamp() {
    return endTimestamp;
  }

  public SearchObjectRequest setEndTimestamp(Long timestamp) {
    this.endTimestamp = timestamp;
    return this;
  }

  @Override
  public TimeMatchStrategy getTimeMatchStrategy() {
    return timeMatchStrategy;
  }

  public SearchObjectRequest setTimeMatchStrategy(TimeMatchStrategy strategy) {
    this.timeMatchStrategy = strategy;
    return this;
  }

  @Override
  public Set<TimeFieldStrategy> getTimeFieldStrategy() {
    return timeFieldStrategy;
  }

  public SearchObjectRequest setTimeFieldStrategy(Set<TimeFieldStrategy> strategy) {
    this.timeFieldStrategy = ObjectUtils.ifNotNull(strategy, SetUtils::set);
    return this;
  }

  public SearchObjectRequest addTimeFieldStrategy(TimeFieldStrategy strategy) {
    this.timeFieldStrategy = SetUtils.addToSet(this.timeFieldStrategy, strategy);
    return this;
  }

  public Integer getMinimumFactsCount() {
    return minimumFactsCount;
  }

  public SearchObjectRequest setMinimumFactsCount(Integer minimumFactsCount) {
    this.minimumFactsCount = minimumFactsCount;
    return this;
  }

  public Integer getMaximumFactsCount() {
    return maximumFactsCount;
  }

  public SearchObjectRequest setMaximumFactsCount(Integer maximumFactsCount) {
    this.maximumFactsCount = maximumFactsCount;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public SearchObjectRequest setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public boolean isIncludeStatistics() {
    return includeStatistics;
  }

  public SearchObjectRequest setIncludeStatistics(boolean includeStatistics) {
    this.includeStatistics = includeStatistics;
    return this;
  }

}
