package no.mnemonic.act.platform.auth.properties.model;

import no.mnemonic.act.platform.auth.properties.internal.IdMapper;
import no.mnemonic.services.common.auth.model.SubjectIdentity;

import java.util.UUID;

/**
 * Identifies a Subject by either a global UUID or its internal numeric ID.
 * <p>
 * Only one ID needs to be set, it will be mapped to the other automatically.
 */
public class SubjectIdentifier implements SubjectIdentity {

  private final UUID globalID;
  private final long internalID;

  private SubjectIdentifier(UUID globalID, long internalID) {
    this.globalID = globalID;
    this.internalID = internalID;
  }

  public UUID getGlobalID() {
    return globalID;
  }

  public long getInternalID() {
    return internalID;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID globalID;
    private long internalID;

    private Builder() {
    }

    public SubjectIdentifier build() {
      return new SubjectIdentifier(globalID, internalID);
    }

    public Builder setGlobalID(UUID globalID) {
      this.globalID = globalID;
      this.internalID = IdMapper.toInternalID(globalID);
      return this;
    }

    public Builder setInternalID(long internalID) {
      this.internalID = internalID;
      this.globalID = IdMapper.toGlobalID(internalID);
      return this;
    }
  }
}
