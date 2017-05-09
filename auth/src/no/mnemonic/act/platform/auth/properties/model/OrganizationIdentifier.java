package no.mnemonic.act.platform.auth.properties.model;

import no.mnemonic.act.platform.auth.properties.internal.IdMapper;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;

import java.util.UUID;

/**
 * Identifies an Organization by either a global UUID or its internal numeric ID.
 * <p>
 * Only one ID needs to be set, it will be mapped to the other automatically.
 */
public class OrganizationIdentifier implements OrganizationIdentity {

  private final UUID globalID;
  private final long internalID;

  private OrganizationIdentifier(UUID globalID, long internalID) {
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

    public OrganizationIdentifier build() {
      return new OrganizationIdentifier(globalID, internalID);
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
