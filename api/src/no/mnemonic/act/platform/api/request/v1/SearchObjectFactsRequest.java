package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;
import java.util.UUID;

public class SearchObjectFactsRequest {

  // Either objectID or objectType + objectValue must be set.
  private UUID objectID;
  private String objectType;
  private String objectValue;
  // Restrict returned Facts bound to one Object.
  private Set<String> factType;
  private Set<String> factValue;
  private Set<String> source;
  private Boolean includeRetracted;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;
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
