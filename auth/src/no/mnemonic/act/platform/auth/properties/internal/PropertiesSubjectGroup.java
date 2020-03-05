package no.mnemonic.act.platform.auth.properties.internal;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Internal representation of a SubjectGroup which holds a set of child Subjects/SubjectGroups.
 */
public class PropertiesSubjectGroup extends PropertiesSubject {

  // Refer to members by internalID.
  private final Set<Long> members;

  private PropertiesSubjectGroup(long internalID, String name, long affiliation, Set<Long> members, Map<Long, Set<String>> permissions) {
    super(internalID, name, affiliation, permissions);
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

  public static class Builder extends PropertiesSubject.Builder<Builder> {
    private Set<Long> members;

    @Override
    public PropertiesSubjectGroup build() {
      return new PropertiesSubjectGroup(internalID, name, affiliation, members, permissions);
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
