package no.mnemonic.services.grafeo.dao.api.criteria;

import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;
import java.util.UUID;

/**
 * Criteria which holds information required for access control during search.
 */
public class AccessControlCriteria {

  private final Set<UUID> currentUserIdentities;
  private final Set<UUID> availableOrganizationID;

  private AccessControlCriteria(Set<UUID> currentUserIdentities, Set<UUID> availableOrganizationID) {
    if (CollectionUtils.isEmpty(currentUserIdentities))
      throw new IllegalArgumentException("Missing required field 'currentUserIdentities'.");
    if (CollectionUtils.isEmpty(availableOrganizationID))
      throw new IllegalArgumentException("Missing required field 'availableOrganizationID'.");

    this.currentUserIdentities = currentUserIdentities;
    this.availableOrganizationID = availableOrganizationID;
  }

  /**
   * Specify the identities of the calling user (ID of the calling user plus any parent groups). This field is required.
   *
   * @return Identities of calling user
   */
  public Set<UUID> getCurrentUserIdentities() {
    return currentUserIdentities;
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
    private Set<UUID> currentUserIdentities;
    private Set<UUID> availableOrganizationID;

    private Builder() {
    }

    public AccessControlCriteria build() {
      return new AccessControlCriteria(currentUserIdentities, availableOrganizationID);
    }

    public Builder setCurrentUserIdentities(Set<UUID> currentUserIdentities) {
      this.currentUserIdentities = currentUserIdentities;
      return this;
    }

    public Builder addCurrentUserIdentity(UUID subjectID) {
      this.currentUserIdentities = SetUtils.addToSet(this.currentUserIdentities, subjectID);
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
