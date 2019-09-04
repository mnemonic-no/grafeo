package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Set;
import java.util.UUID;

@ApiModel(description = "Search for Objects.")
public class SearchObjectRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Only return Objects matching a keyword query")
  private String keywords;
  @ApiModelProperty(value = "Only return Objects identified by their UUID")
  private Set<UUID> objectID;
  @ApiModelProperty(value = "Only return Objects bound to specific Facts")
  private Set<UUID> factID;
  @ApiModelProperty(value = "Only return Objects with a specific ObjectType")
  private Set<String> objectType;
  @ApiModelProperty(value = "Only return Objects with Facts having a specific FactType")
  private Set<String> factType;
  @ApiModelProperty(value = "Only return Objects matching a specific value")
  private Set<String> objectValue;
  @ApiModelProperty(value = "Only return Objects with Facts matching a specific value")
  private Set<String> factValue;
  @ApiModelProperty(value = "Only return Objects with Facts belonging to a specific Organization")
  private Set<String> organization;
  @ApiModelProperty(value = "Only return Objects with Facts coming from a specific Origin")
  private Set<String> origin;
  @ApiModelProperty(value = "Only return Objects with Facts where the field specified by 'dimension' is above a specific value (value between 0.0 and 1.0)")
  @Min(0)
  @Max(1)
  private Float minimum;
  @ApiModelProperty(value = "Only return Objects with Facts where the field specified by 'dimension' is below a specific value (value between 0.0 and 1.0)")
  @Min(0)
  @Max(1)
  private Float maximum;
  @ApiModelProperty(value = "Specify the field used for minimum/maximum filters (default 'certainty')")
  private Dimension dimension;
  @ApiModelProperty(value = "Only return Objects with Facts added before a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only return Objects with Facts added after a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
  @ApiModelProperty(value = "Limit the number of returned Objects (default 25, 0 means all)", example = "25")
  @Min(0)
  private Integer limit;

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
