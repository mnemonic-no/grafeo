package no.mnemonic.act.platform.dao.api;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;
import java.util.UUID;

/**
 * Criteria to verify if a Fact logically already exists in the system (i.e. indexed in ElasticSearch).
 */
public class FactExistenceSearchCriteria {

  public enum AccessMode {
    Public, RoleBased, Explicit
  }

  public enum Direction {
    FactIsSource, FactIsDestination, BiDirectional
  }

  private final String factValue;
  private final UUID factTypeID;
  private final UUID sourceID;
  private final UUID organizationID;
  private final AccessMode accessMode;
  private final Float confidence;
  private final UUID inReferenceTo;
  private final Set<ObjectExistence> objects;

  private FactExistenceSearchCriteria(String factValue,
                                      UUID factTypeID,
                                      UUID sourceID,
                                      UUID organizationID,
                                      AccessMode accessMode,
                                      Float confidence,
                                      UUID inReferenceTo,
                                      Set<ObjectExistence> objects) {
    this.factValue = factValue; // Field 'factValue' is optional.
    this.factTypeID = ObjectUtils.notNull(factTypeID, "Missing required field 'factTypeID'.");
    this.sourceID = ObjectUtils.notNull(sourceID, "Missing required field 'sourceID'.");
    this.organizationID = ObjectUtils.notNull(organizationID, "Missing required field 'organizationID'.");
    this.accessMode = ObjectUtils.notNull(accessMode, "Missing required field 'accessMode'.");
    this.confidence = ObjectUtils.notNull(confidence, "Missing required field 'confidence'.");
    this.inReferenceTo = inReferenceTo;
    this.objects = objects;

    if (inReferenceTo == null && CollectionUtils.isEmpty(objects)) {
      throw new IllegalArgumentException("Either 'inReferenceTo' or 'objects' must be set.");
    }
    if (inReferenceTo != null && !CollectionUtils.isEmpty(objects)) {
      throw new IllegalArgumentException("Only one of 'inReferenceTo' or 'objects' must be set.");
    }
  }

  /**
   * Value of Fact to verify. This field is optional.
   *
   * @return Value of Fact
   */
  public String getFactValue() {
    return factValue;
  }

  /**
   * FactType of Fact to verify (by UUID). This field is required.
   *
   * @return UUID of FactType
   */
  public UUID getFactTypeID() {
    return factTypeID;
  }

  /**
   * Source of Fact to verify (by UUID). This field is required.
   *
   * @return UUID of Source
   */
  public UUID getSourceID() {
    return sourceID;
  }

  /**
   * Organization of Fact to verify (by UUID). This field is required.
   *
   * @return UUID of Organization
   */
  public UUID getOrganizationID() {
    return organizationID;
  }

  /**
   * AccessMode of Fact to verify. This field is required.
   *
   * @return AccessMode of Fact
   */
  public AccessMode getAccessMode() {
    return accessMode;
  }

  /**
   * Confidence of Fact to verify. This field is required.
   *
   * @return Confidence of Fact
   */
  public Float getConfidence() {
    return confidence;
  }

  /**
   * Referenced Fact of Fact to verify (used for meta Facts).
   *
   * @return UUID of referenced Fact
   */
  public UUID getInReferenceTo() {
    return inReferenceTo;
  }

  /**
   * Objects bound to Fact to verify (used for regular Facts).
   *
   * @return Bound Objects
   */
  public Set<ObjectExistence> getObjects() {
    return objects;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String factValue;
    private UUID factTypeID;
    private UUID sourceID;
    private UUID organizationID;
    private AccessMode accessMode;
    private Float confidence;
    private UUID inReferenceTo;
    private Set<ObjectExistence> objects;

    private Builder() {
    }

    public FactExistenceSearchCriteria build() {
      return new FactExistenceSearchCriteria(factValue, factTypeID, sourceID, organizationID, accessMode, confidence, inReferenceTo, objects);
    }

    public Builder setFactValue(String factValue) {
      this.factValue = factValue;
      return this;
    }

    public Builder setFactTypeID(UUID factTypeID) {
      this.factTypeID = factTypeID;
      return this;
    }

    public Builder setSourceID(UUID sourceID) {
      this.sourceID = sourceID;
      return this;
    }

    public Builder setOrganizationID(UUID organizationID) {
      this.organizationID = organizationID;
      return this;
    }

    public Builder setAccessMode(String accessMode) {
      this.accessMode = AccessMode.valueOf(accessMode);
      return this;
    }

    public Builder setConfidence(Float confidence) {
      this.confidence = confidence;
      return this;
    }

    public Builder setInReferenceTo(UUID inReferenceTo) {
      this.inReferenceTo = inReferenceTo;
      return this;
    }

    public Builder addObject(UUID objectID, String direction) {
      this.objects = SetUtils.addToSet(this.objects, new ObjectExistence(objectID, Direction.valueOf(direction)));
      return this;
    }
  }

  /**
   * Used for verifying the existence of Objects bound to a Fact.
   */
  public static class ObjectExistence {
    private final UUID objectID;
    private final Direction direction;

    private ObjectExistence(UUID objectID, Direction direction) {
      this.objectID = ObjectUtils.notNull(objectID, "Missing required field 'objectID'.");
      this.direction = ObjectUtils.notNull(direction, "Missing required field 'direction'.");
    }

    /**
     * UUID of Object to verify. This field is required.
     *
     * @return UUID of Object
     */
    public UUID getObjectID() {
      return objectID;
    }

    /**
     * Direction of binding between Object and Fact. This field is required.
     *
     * @return Direction of binding
     */
    public Direction getDirection() {
      return direction;
    }
  }
}
