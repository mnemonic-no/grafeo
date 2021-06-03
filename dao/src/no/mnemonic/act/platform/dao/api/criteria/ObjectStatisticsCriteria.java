package no.mnemonic.act.platform.dao.api.criteria;

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
  private final UUID currentUserID;
  private final Set<UUID> availableOrganizationID;

  private ObjectStatisticsCriteria(Set<UUID> objectID,
                                   Long startTimestamp,
                                   Long endTimestamp,
                                   UUID currentUserID,
                                   Set<UUID> availableOrganizationID) {
    if (CollectionUtils.isEmpty(objectID)) throw new IllegalArgumentException("Missing required field 'objectID'.");
    if (currentUserID == null) throw new IllegalArgumentException("Missing required field 'currentUserID'.");
    if (CollectionUtils.isEmpty(availableOrganizationID))
      throw new IllegalArgumentException("Missing required field 'availableOrganizationID'.");

    this.objectID = objectID;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.currentUserID = currentUserID;
    this.availableOrganizationID = availableOrganizationID;
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
   * Specify the UUID of the calling user. This field is required.
   *
   * @return UUID of calling user
   */
  public UUID getCurrentUserID() {
    return currentUserID;
  }

  /**
   * Specify the Organizations the calling user has access to (by UUID). This field is required.
   *
   * @return UUIDs of available Organizations
   */
  public Set<UUID> getAvailableOrganizationID() {
    return availableOrganizationID;
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
    private UUID currentUserID;
    private Set<UUID> availableOrganizationID;

    private Builder() {
    }

    public ObjectStatisticsCriteria build() {
      return new ObjectStatisticsCriteria(objectID, startTimestamp, endTimestamp, currentUserID, availableOrganizationID);
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

    public Builder setCurrentUserID(UUID currentUserID) {
      this.currentUserID = currentUserID;
      return this;
    }

    public Builder setAvailableOrganizationID(Set<UUID> availableOrganizationID) {
      this.availableOrganizationID = availableOrganizationID;
      return this;
    }

    public Builder addAvailableOrganizationID(UUID availableOrganizationID) {
      this.availableOrganizationID = SetUtils.addToSet(this.availableOrganizationID, availableOrganizationID);
      return this;
    }
  }
}
