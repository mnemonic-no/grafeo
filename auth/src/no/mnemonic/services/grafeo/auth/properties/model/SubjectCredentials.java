package no.mnemonic.services.grafeo.auth.properties.model;

import no.mnemonic.services.common.auth.model.Credentials;

/**
 * Credentials based on identifying a Subject by its numeric internal ID.
 */
public class SubjectCredentials implements Credentials {

  private final long subjectID;

  private SubjectCredentials(long subjectID) {
    this.subjectID = subjectID;
  }

  public long getSubjectID() {
    return subjectID;
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
