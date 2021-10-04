package no.mnemonic.act.platform.dao.api.criteria;

import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;
import java.util.UUID;

/**
 * Criteria which holds information required for access control during search.
 */
public class AccessControlCriteria {

  private final UUID currentUserID;
  private final Set<UUID> availableOrganizationID;

  private AccessControlCriteria(UUID currentUserID, Set<UUID> availableOrganizationID) {
    if (currentUserID == null) throw new IllegalArgumentException("Missing required field 'currentUserID'.");
    if (CollectionUtils.isEmpty(availableOrganizationID))
      throw new IllegalArgumentException("Missing required field 'availableOrganizationID'.");

    this.currentUserID = currentUserID;
    this.availableOrganizationID = availableOrganizationID;
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
    private UUID currentUserID;
    private Set<UUID> availableOrganizationID;

    private Builder() {
    }

    public AccessControlCriteria build() {
      return new AccessControlCriteria(currentUserID, availableOrganizationID);
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
