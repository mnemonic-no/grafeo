package no.mnemonic.services.grafeo.dao.api.criteria;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Criteria used for searching for Facts in ElasticSearch.
 */
public class FactSearchCriteria {

  public enum MatchStrategy {
    all, any
  }

  public interface FieldStrategy {
    Set<String> getFields();
  }

  public enum TimeFieldStrategy implements FieldStrategy {
    timestamp("timestamp"),
    lastSeenTimestamp("lastSeenTimestamp"),
    all("timestamp", "lastSeenTimestamp");

    private final Set<String> fields;

    TimeFieldStrategy(String... fields) {
      this.fields = Collections.unmodifiableSet(SetUtils.set(fields));
    }

    @Override
    public Set<String> getFields() {
      return fields;
    }
  }

  public enum KeywordFieldStrategy implements FieldStrategy {
    factValueText("value.text"),
    factValueIp("value.ip"),
    factValueDomain("value.domain"),
    objectValueText("objects.value.text"),
    objectValueIp("objects.value.ip"),
    objectValueDomain("objects.value.domain"),
    all("value.text", "value.ip", "value.domain", "objects.value.text", "objects.value.ip", "objects.value.domain");

    private final Set<String> fields;

    KeywordFieldStrategy(String... fields) {
      this.fields = Collections.unmodifiableSet(SetUtils.set(fields));
    }

    @Override
    public Set<String> getFields() {
      return fields;
    }
  }

  public enum NumberFieldStrategy implements FieldStrategy {
    trust("trust"),
    confidence("confidence"),
    certainty("certainty"),
    all("trust", "confidence", "certainty");

    private final Set<String> fields;

    NumberFieldStrategy(String... fields) {
      this.fields = Collections.unmodifiableSet(SetUtils.set(fields));
    }

    @Override
    public Set<String> getFields() {
      return fields;
    }
  }

  public enum FactBinding {
    meta(0), oneLegged(1), twoLegged(2);

    private final int objectCount;

    FactBinding(int objectCount) {
      this.objectCount = objectCount;
    }

    public int getObjectCount() {
      return objectCount;
    }
  }

  // Filter returned Facts based on those fields.
  private final Set<UUID> factID;
  private final Set<UUID> factTypeID;
  private final Set<String> factValue;
  private final Set<UUID> inReferenceTo;
  private final Set<UUID> organizationID;
  private final Set<UUID> originID;
  private final Set<UUID> objectID;
  private final Set<UUID> objectTypeID;
  private final Set<String> objectValue;

  // Keywords search.
  private final String keywords;
  private final Set<KeywordFieldStrategy> keywordFieldStrategy;
  private final MatchStrategy keywordMatchStrategy;

  // Timestamp search.
  private final Long startTimestamp;
  private final Long endTimestamp;
  private final Set<TimeFieldStrategy> timeFieldStrategy;
  private final MatchStrategy timeMatchStrategy;

  // Number search.
  private final Number minNumber;
  private final Number maxNumber;
  private final Set<NumberFieldStrategy> numberFieldStrategy;
  private final MatchStrategy numberMatchStrategy;

  // Additional search options.
  private final FactBinding factBinding;
  private final Integer minimumFactsCount;
  private final Integer maximumFactsCount;
  private final int limit;

  // Fields required for access control.
  private final AccessControlCriteria accessControlCriteria;

  // Fields required for selecting the indices to query.
  private final IndexSelectCriteria indexSelectCriteria;

  private FactSearchCriteria(Set<UUID> factID,
                             Set<UUID> factTypeID,
                             Set<String> factValue,
                             Set<UUID> inReferenceTo,
                             Set<UUID> organizationID,
                             Set<UUID> originID,
                             Set<UUID> objectID,
                             Set<UUID> objectTypeID,
                             Set<String> objectValue,
                             String keywords,
                             Set<KeywordFieldStrategy> keywordFieldStrategy,
                             MatchStrategy keywordMatchStrategy,
                             Long startTimestamp,
                             Long endTimestamp,
                             Set<TimeFieldStrategy> timeFieldStrategy,
                             MatchStrategy timeMatchStrategy,
                             Number minNumber,
                             Number maxNumber,
                             Set<NumberFieldStrategy> numberFieldStrategy,
                             MatchStrategy numberMatchStrategy,
                             FactBinding factBinding,
                             Integer minimumFactsCount,
                             Integer maximumFactsCount,
                             int limit,
                             AccessControlCriteria accessControlCriteria,
                             IndexSelectCriteria indexSelectCriteria) {
    if (accessControlCriteria == null) throw new IllegalArgumentException("Missing required field 'accessControlCriteria'.");
    if (indexSelectCriteria == null) throw new IllegalArgumentException("Missing required field 'indexSelectCriteria'.");

    this.factID = factID;
    this.factTypeID = factTypeID;
    this.factValue = factValue;
    this.inReferenceTo = inReferenceTo;
    this.organizationID = organizationID;
    this.originID = originID;
    this.objectID = objectID;
    this.objectTypeID = objectTypeID;
    this.objectValue = objectValue;
    this.keywords = keywords;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.minNumber = minNumber;
    this.maxNumber = maxNumber;
    this.factBinding = factBinding;
    this.minimumFactsCount = minimumFactsCount;
    this.maximumFactsCount = maximumFactsCount;
    this.limit = limit;
    this.accessControlCriteria = accessControlCriteria;
    this.indexSelectCriteria = indexSelectCriteria;

    // Set default values for strategies if not provided by user.
    this.keywordFieldStrategy = !CollectionUtils.isEmpty(keywordFieldStrategy) ? keywordFieldStrategy :
            SetUtils.set(KeywordFieldStrategy.all);
    this.keywordMatchStrategy = ObjectUtils.ifNull(keywordMatchStrategy, MatchStrategy.any);
    this.timeFieldStrategy = !CollectionUtils.isEmpty(timeFieldStrategy) ? timeFieldStrategy :
            SetUtils.set(TimeFieldStrategy.all);
    this.timeMatchStrategy = ObjectUtils.ifNull(timeMatchStrategy, MatchStrategy.any);
    this.numberFieldStrategy = !CollectionUtils.isEmpty(numberFieldStrategy) ? numberFieldStrategy :
            SetUtils.set(NumberFieldStrategy.all);
    this.numberMatchStrategy = ObjectUtils.ifNull(numberMatchStrategy, MatchStrategy.any);
  }

  /**
   * Filter Facts by their UUID.
   *
   * @return UUIDs of Facts
   */
  public Set<UUID> getFactID() {
    return factID;
  }

  /**
   * Filter Facts by their FactType (by UUID).
   *
   * @return UUIDs of FactTypes
   */
  public Set<UUID> getFactTypeID() {
    return factTypeID;
  }

  /**
   * Filter Facts by their value (exact match).
   *
   * @return Values of Facts
   */
  public Set<String> getFactValue() {
    return factValue;
  }

  /**
   * Filter Facts by their reference to other Facts.
   *
   * @return UUIDs of referenced Facts
   */
  public Set<UUID> getInReferenceTo() {
    return inReferenceTo;
  }

  /**
   * Filter Facts by their Organization (by UUID).
   *
   * @return UUIDs of Organizations
   */
  public Set<UUID> getOrganizationID() {
    return organizationID;
  }

  /**
   * Filter Facts by their Origin (by UUID).
   *
   * @return UUIDs of Origins
   */
  public Set<UUID> getOriginID() {
    return originID;
  }

  /**
   * Filter Facts by their bound Objects (by Object UUID).
   *
   * @return UUIDs of bound Objects
   */
  public Set<UUID> getObjectID() {
    return objectID;
  }

  /**
   * Filter Facts by their bound Objects (by ObjectType UUID).
   *
   * @return UUIDs of ObjectTypes
   */
  public Set<UUID> getObjectTypeID() {
    return objectTypeID;
  }

  /**
   * Filter Facts by their bound Objects (by Object value; exact match).
   *
   * @return Values of bound Objects
   */
  public Set<String> getObjectValue() {
    return objectValue;
  }

  /**
   * Filter Facts by a keyword search string (using ElasticSearches simple query string).
   *
   * @return Keyword search string
   */
  public String getKeywords() {
    return keywords;
  }

  /**
   * Specify against which fields the keyword search will be executed (defaults to 'all').
   *
   * @return Fields to execute keyword search against
   */
  public Set<KeywordFieldStrategy> getKeywordFieldStrategy() {
    return keywordFieldStrategy;
  }

  /**
   * Specify how the keyword search will be executed (defaults to 'any').
   * <p>
   * any: The keyword search string must match at least one field defined in the KeywordFieldStrategy.
   * all: The keyword search string must match all fields defined in the KeywordFieldStrategy.
   *
   * @return How the keyword search will be executed
   */
  public MatchStrategy getKeywordMatchStrategy() {
    return keywordMatchStrategy;
  }

  /**
   * Filter Facts by timestamp (start).
   *
   * @return Start of timestamp search
   */
  public Long getStartTimestamp() {
    return startTimestamp;
  }

  /**
   * Filter Facts by timestamp (end).
   *
   * @return End of timestamp search
   */
  public Long getEndTimestamp() {
    return endTimestamp;
  }

  /**
   * Specify against which fields the timestamp search will be executed (defaults to 'all').
   *
   * @return Fields to execute timestamp search against
   */
  public Set<TimeFieldStrategy> getTimeFieldStrategy() {
    return timeFieldStrategy;
  }

  /**
   * Specify how the timestamp search will be executed (defaults to 'any').
   * <p>
   * any: At least one timestamp defined in the TimestampFieldStrategy must be between startTimestamp and endTimestamp.
   * all: All timestamps defined in the TimestampFieldStrategy must be between startTimestamp and endTimestamp.
   *
   * @return How the timestamp search will be executed
   */
  public MatchStrategy getTimeMatchStrategy() {
    return timeMatchStrategy;
  }

  /**
   * Filter Facts by number (minimum value)
   *
   * @return Minimum for number search
   */
  public Number getMinNumber() {
    return minNumber;
  }

  /**
   * Filter Facts by number (maximum value)
   *
   * @return Maximum for number search
   */
  public Number getMaxNumber() {
    return maxNumber;
  }

  /**
   * Specify against which fields the number search will be executed (defaults to 'all').
   *
   * @return Fields to execute number search against
   */
  public Set<NumberFieldStrategy> getNumberFieldStrategy() {
    return numberFieldStrategy;
  }

  /**
   * Specify how the number search will be executed (defaults to 'any').
   * <p>
   * any: At least one number defined in the NumberFieldStrategy must be between minNumber and maxNumber.
   * all: All numbers defined in the NumberFieldStrategy must be between minNumber and maxNumber.
   *
   * @return How the number search will be executed
   */
  public MatchStrategy getNumberMatchStrategy() {
    return numberMatchStrategy;
  }

  /**
   * Filter by fact binding, either one-legged, two-legged or meta-fact.
   *
   * @return The fact binding to filter by
   */
  public FactBinding getFactBinding() {
    return factBinding;
  }

  /**
   * Filter Objects which have a minimum amount of bound Facts.
   * <p>
   * The amount of bound Facts is calculated after filtering out Facts. Note that this filter is only applied to Object search.
   *
   * @return Minimum amount of bound Facts
   */
  public Integer getMinimumFactsCount() {
    return minimumFactsCount;
  }

  /**
   * Filter Objects which have a maximum amount of bound Facts.
   * <p>
   * The amount of bound Facts is calculated after filtering out Facts. Note that this filter is only applied to Object search.
   *
   * @return Maximum amount of bound Facts
   */
  public Integer getMaximumFactsCount() {
    return maximumFactsCount;
  }

  /**
   * Restrict the maximum amount of returned Facts. The amount actually returned might be smaller.
   *
   * @return Maximum amount of returned Facts
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Specify criteria required for access control. This field is required.
   *
   * @return Access control criteria
   */
  public AccessControlCriteria getAccessControlCriteria() {
    return accessControlCriteria;
  }

  /**
   * Specify criteria to decide which indices in ElasticSearch will be queried. This field is required.
   *
   * @return Index select criteria
   */
  public IndexSelectCriteria getIndexSelectCriteria() {
    return indexSelectCriteria;
  }

  /**
   * Returns true if the criteria is effectively unbounded, i.e. no criteria is specified which would reduce the search result.
   * Note that 'limit' isn't included here which means that true will be returned if only 'limit' is given.
   *
   * @return Whether the criteria is effectively unbounded
   */
  public boolean isUnbounded() {
    return CollectionUtils.isEmpty(factID) &&
            CollectionUtils.isEmpty(factTypeID) &&
            CollectionUtils.isEmpty(factValue) &&
            CollectionUtils.isEmpty(inReferenceTo) &&
            CollectionUtils.isEmpty(organizationID) &&
            CollectionUtils.isEmpty(originID) &&
            CollectionUtils.isEmpty(objectID) &&
            CollectionUtils.isEmpty(objectTypeID) &&
            CollectionUtils.isEmpty(objectValue) &&
            StringUtils.isBlank(keywords) &&
            startTimestamp == null &&
            endTimestamp == null &&
            minNumber == null &&
            maxNumber == null &&
            minimumFactsCount == null &&
            maximumFactsCount == null;
  }

  /**
   * Create a new builder instance from an existing FactSearchCriteria object. All fields set in the criteria will be
   * copied to the new builder. Useful for creating a new criteria from a given criteria with only small modifications.
   *
   * @return New builder instance
   */
  public Builder toBuilder() {
    return new Builder()
            .setFactID(factID)
            .setFactTypeID(factTypeID)
            .setFactValue(factValue)
            .setInReferenceTo(inReferenceTo)
            .setOrganizationID(organizationID)
            .setOriginID(originID)
            .setObjectID(objectID)
            .setObjectTypeID(objectTypeID)
            .setObjectValue(objectValue)
            .setKeywords(keywords)
            .setKeywordFieldStrategy(keywordFieldStrategy)
            .setKeywordMatchStrategy(keywordMatchStrategy)
            .setStartTimestamp(startTimestamp)
            .setEndTimestamp(endTimestamp)
            .setTimeFieldStrategy(timeFieldStrategy)
            .setTimeMatchStrategy(timeMatchStrategy)
            .setMinNumber(minNumber)
            .setMaxNumber(maxNumber)
            .setNumberFieldStrategy(numberFieldStrategy)
            .setNumberMatchStrategy(numberMatchStrategy)
            .setFactBinding(factBinding)
            .setMinimumFactsCount(minimumFactsCount)
            .setMaximumFactsCount(maximumFactsCount)
            .setLimit(limit)
            .setAccessControlCriteria(accessControlCriteria)
            .setIndexSelectCriteria(indexSelectCriteria);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    // Filter returned Facts based on those fields.
    private Set<UUID> factID;
    private Set<UUID> factTypeID;
    private Set<String> factValue;
    private Set<UUID> inReferenceTo;
    private Set<UUID> organizationID;
    private Set<UUID> originID;
    private Set<UUID> objectID;
    private Set<UUID> objectTypeID;
    private Set<String> objectValue;

    // Keywords search.
    private String keywords;
    private Set<KeywordFieldStrategy> keywordFieldStrategy;
    private MatchStrategy keywordMatchStrategy;

    // Timestamp search.
    private Long startTimestamp;
    private Long endTimestamp;
    private Set<TimeFieldStrategy> timeFieldStrategy;
    private MatchStrategy timeMatchStrategy;

    // Numerical search.
    private Number minNumber;
    private Number maxNumber;
    private Set<NumberFieldStrategy> numberFieldStrategy;
    private MatchStrategy numberMatchStrategy;

    // Additional search options.
    private FactBinding factBinding;
    private Integer minimumFactsCount;
    private Integer maximumFactsCount;
    private int limit;

    // Fields required for access control.
    private AccessControlCriteria accessControlCriteria;

    // Fields required for selecting the indices to query.
    private IndexSelectCriteria indexSelectCriteria;

    private Builder() {
    }

    public FactSearchCriteria build() {
      return new FactSearchCriteria(factID, factTypeID, factValue, inReferenceTo, organizationID, originID, objectID, objectTypeID,
              objectValue, keywords, keywordFieldStrategy, keywordMatchStrategy, startTimestamp, endTimestamp, timeFieldStrategy,
              timeMatchStrategy, minNumber, maxNumber, numberFieldStrategy, numberMatchStrategy, factBinding, minimumFactsCount,
              maximumFactsCount, limit, accessControlCriteria, indexSelectCriteria);
    }

    public Builder setFactID(Set<UUID> factID) {
      this.factID = factID;
      return this;
    }

    public Builder addFactID(UUID factID) {
      this.factID = SetUtils.addToSet(this.factID, factID);
      return this;
    }

    public Builder setFactTypeID(Set<UUID> factTypeID) {
      this.factTypeID = factTypeID;
      return this;
    }

    public Builder addFactTypeID(UUID factTypeID) {
      this.factTypeID = SetUtils.addToSet(this.factTypeID, factTypeID);
      return this;
    }

    public Builder setFactValue(Set<String> factValue) {
      this.factValue = factValue;
      return this;
    }

    public Builder addFactValue(String factValue) {
      this.factValue = SetUtils.addToSet(this.factValue, factValue);
      return this;
    }

    public Builder setInReferenceTo(Set<UUID> inReferenceTo) {
      this.inReferenceTo = inReferenceTo;
      return this;
    }

    public Builder addInReferenceTo(UUID inReferenceTo) {
      this.inReferenceTo = SetUtils.addToSet(this.inReferenceTo, inReferenceTo);
      return this;
    }

    public Builder setOrganizationID(Set<UUID> organizationID) {
      this.organizationID = organizationID;
      return this;
    }

    public Builder addOrganizationID(UUID organizationID) {
      this.organizationID = SetUtils.addToSet(this.organizationID, organizationID);
      return this;
    }

    public Builder setOriginID(Set<UUID> originID) {
      this.originID = originID;
      return this;
    }

    public Builder addOriginID(UUID originID) {
      this.originID = SetUtils.addToSet(this.originID, originID);
      return this;
    }

    public Builder setObjectID(Set<UUID> objectID) {
      this.objectID = objectID;
      return this;
    }

    public Builder addObjectID(UUID objectID) {
      this.objectID = SetUtils.addToSet(this.objectID, objectID);
      return this;
    }

    public Builder setObjectTypeID(Set<UUID> objectTypeID) {
      this.objectTypeID = objectTypeID;
      return this;
    }

    public Builder addObjectTypeID(UUID objectTypeID) {
      this.objectTypeID = SetUtils.addToSet(this.objectTypeID, objectTypeID);
      return this;
    }

    public Builder setObjectValue(Set<String> objectValue) {
      this.objectValue = objectValue;
      return this;
    }

    public Builder addObjectValue(String objectValue) {
      this.objectValue = SetUtils.addToSet(this.objectValue, objectValue);
      return this;
    }

    public Builder setKeywords(String keywords) {
      this.keywords = keywords;
      return this;
    }

    public Builder setKeywordFieldStrategy(Set<KeywordFieldStrategy> keywordFieldStrategy) {
      this.keywordFieldStrategy = keywordFieldStrategy;
      return this;
    }

    public Builder addKeywordFieldStrategy(KeywordFieldStrategy keywordFieldStrategy) {
      this.keywordFieldStrategy = SetUtils.addToSet(this.keywordFieldStrategy, keywordFieldStrategy);
      return this;
    }

    public Builder setKeywordMatchStrategy(MatchStrategy keywordMatchStrategy) {
      this.keywordMatchStrategy = keywordMatchStrategy;
      return this;
    }

    public Builder setStartTimestamp(Long startTimestamp) {
      this.startTimestamp = startTimestamp;
      return this;
    }

    public Builder setEndTimestamp(Long endTimestamp) {
      this.endTimestamp = endTimestamp;
      return this;
    }

    public Builder setTimeFieldStrategy(Set<TimeFieldStrategy> timeFieldStrategy) {
      this.timeFieldStrategy = timeFieldStrategy;
      return this;
    }

    public Builder addTimeFieldStrategy(TimeFieldStrategy timeFieldStrategy) {
      this.timeFieldStrategy = SetUtils.addToSet(this.timeFieldStrategy, timeFieldStrategy);
      return this;
    }

    public Builder setTimeMatchStrategy(MatchStrategy timeMatchStrategy) {
      this.timeMatchStrategy = timeMatchStrategy;
      return this;
    }

    public Builder setMinNumber(Number minNumber) {
      this.minNumber = minNumber;
      return this;
    }

    public Builder setMaxNumber(Number maxNumber) {
      this.maxNumber = maxNumber;
      return this;
    }

    public Builder setNumberFieldStrategy(Set<NumberFieldStrategy> numberFieldStrategy) {
      this.numberFieldStrategy = numberFieldStrategy;
      return this;
    }

    public Builder addNumberFieldStrategy(NumberFieldStrategy numberFieldStrategy) {
      this.numberFieldStrategy = SetUtils.addToSet(this.numberFieldStrategy, numberFieldStrategy);
      return this;
    }

    public Builder setNumberMatchStrategy(MatchStrategy numberMatchStrategy) {
      this.numberMatchStrategy = numberMatchStrategy;
      return this;
    }

    public Builder setFactBinding(FactBinding factBinding) {
      this.factBinding = factBinding;
      return this;
    }

    public Builder setMinimumFactsCount(Integer minimumFactsCount) {
      this.minimumFactsCount = minimumFactsCount;
      return this;
    }

    public Builder setMaximumFactsCount(Integer maximumFactsCount) {
      this.maximumFactsCount = maximumFactsCount;
      return this;
    }

    public Builder setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder setAccessControlCriteria(AccessControlCriteria accessControlCriteria) {
      this.accessControlCriteria = accessControlCriteria;
      return this;
    }

    public Builder setIndexSelectCriteria(IndexSelectCriteria indexSelectCriteria) {
      this.indexSelectCriteria = indexSelectCriteria;
      return this;
    }
  }
}
