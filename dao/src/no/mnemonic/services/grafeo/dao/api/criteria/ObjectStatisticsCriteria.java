package no.mnemonic.services.grafeo.dao.api.criteria;

import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;
import java.util.UUID;

/**
 * Criteria used for calculating statistics about Facts bound to specific Objects.
 */
public class ObjectStatisticsCriteria {

  // Return statistics for those Objects.
  private final Set<UUID> objectID;

  // Only calculate statistics for Facts last seen within the given time frame.
  private final Long startTimestamp;
  private final Long endTimestamp;

  // Fields required for access control on related Facts.
  private final AccessControlCriteria accessControlCriteria;

  // Fields required for selecting the indices to query.
  private final IndexSelectCriteria indexSelectCriteria;

  private ObjectStatisticsCriteria(Set<UUID> objectID,
                                   Long startTimestamp,
                                   Long endTimestamp,
                                   AccessControlCriteria accessControlCriteria,
                                   IndexSelectCriteria indexSelectCriteria) {
    if (CollectionUtils.isEmpty(objectID)) throw new IllegalArgumentException("Missing required field 'objectID'.");
    if (accessControlCriteria == null) throw new IllegalArgumentException("Missing required field 'accessControlCriteria'.");
    if (indexSelectCriteria == null) throw new IllegalArgumentException("Missing required field 'indexSelectCriteria'.");

    this.objectID = objectID;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.accessControlCriteria = accessControlCriteria;
    this.indexSelectCriteria = indexSelectCriteria;
  }

  /**
   * Specify for which Objects statistics should be calculated (by Object UUID). This field is required.
   *
   * @return UUIDs of Objects
   */
  public Set<UUID> getObjectID() {
    return objectID;
  }

  /**
   * Only calculate statistics for Facts last seen within the given time frame (start). This field is optional.
   *
   * @return Start of time frame
   */
  public Long getStartTimestamp() {
    return startTimestamp;
  }

  /**
   * Only calculate statistics for Facts last seen within the given time frame (end). This field is optional.
   *
   * @return End of time frame
   */
  public Long getEndTimestamp() {
    return endTimestamp;
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    // Return statistics for those Objects.
    private Set<UUID> objectID;

    // Only calculate statistics for Facts last seen within the given time frame.
    private Long startTimestamp;
    private Long endTimestamp;

    // Fields required for access control on related Facts.
    private AccessControlCriteria accessControlCriteria;

    // Fields required for selecting the indices to query.
    private IndexSelectCriteria indexSelectCriteria;

    private Builder() {
    }

    public ObjectStatisticsCriteria build() {
      return new ObjectStatisticsCriteria(objectID, startTimestamp, endTimestamp, accessControlCriteria, indexSelectCriteria);
    }

    public Builder setObjectID(Set<UUID> objectID) {
      this.objectID = objectID;
      return this;
    }

    public Builder addObjectID(UUID objectID) {
      this.objectID = SetUtils.addToSet(this.objectID, objectID);
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
