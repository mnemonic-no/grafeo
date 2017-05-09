package no.mnemonic.act.platform.auth.properties.model;

import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.SecurityLevel;
import no.mnemonic.services.common.auth.model.SubjectIdentity;

/**
 * Credentials based on identifying a Subject by mapping its numeric internal ID to a SubjectIdentifier.
 */
public class SubjectCredentials implements Credentials {

  private final SubjectIdentifier identifier;

  private SubjectCredentials(long subjectID) {
    identifier = SubjectIdentifier.builder()
            .setInternalID(subjectID)
            .build();
  }

  @Override
  public SubjectIdentity getUserID() {
    return identifier;
  }

  @Override
  public SecurityLevel getSecurityLevel() {
    throw new UnsupportedOperationException("Security level is not implemented!");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long subjectID;

    private Builder() {
    }

    public SubjectCredentials build() {
      return new SubjectCredentials(subjectID);
    }

    public Builder setSubjectID(long subjectID) {
      this.subjectID = subjectID;
      return this;
    }
  }
}
