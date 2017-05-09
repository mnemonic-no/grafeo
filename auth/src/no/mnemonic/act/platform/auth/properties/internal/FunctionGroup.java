package no.mnemonic.act.platform.auth.properties.internal;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Set;

/**
 * Internal representation of a FunctionGroup which holds a set of child Functions/FunctionGroups.
 */
public class FunctionGroup extends Function {

  // Refer to members by function name.
  private final Set<String> members;

  private FunctionGroup(String name, Set<String> members) {
    super(name);
    this.members = ObjectUtils.ifNotNull(members, Collections::unmodifiableSet, Collections.emptySet());
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  public Set<String> getMembers() {
    return members;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Function.Builder<Builder> {
    private Set<String> members;

    @Override
    public FunctionGroup build() {
      return new FunctionGroup(name, members);
    }

    public Builder setMembers(Set<String> members) {
      this.members = members;
      return this;
    }

    public Builder addMember(String member) {
      this.members = SetUtils.addToSet(this.members, member);
      return this;
    }
  }
}
