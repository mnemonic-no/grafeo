package no.mnemonic.act.platform.auth.properties.internal;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Set;

/**
 * Internal representation of an OrganizationGroup which holds a set of child Organizations/OrganizationGroups.
 */
public class PropertiesOrganizationGroup extends PropertiesOrganization {

  // Refer to members by internalID.
  private final Set<Long> members;

  private PropertiesOrganizationGroup(long internalID, String name, Set<Long> members) {
    super(internalID, name);
    this.members = ObjectUtils.ifNotNull(members, Collections::unmodifiableSet, Collections.emptySet());
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  public Set<Long> getMembers() {
    return members;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends PropertiesOrganization.Builder<Builder> {
    private Set<Long> members;

    @Override
    public PropertiesOrganizationGroup build() {
      return new PropertiesOrganizationGroup(internalID, name, members);
    }

    public Builder setMembers(Set<Long> members) {
      this.members = members;
      return this;
    }

    public Builder addMember(long member) {
      this.members = SetUtils.addToSet(this.members, member);
      return this;
    }
  }
}
