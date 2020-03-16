package no.mnemonic.act.platform.auth.properties.model;

import no.mnemonic.services.common.auth.model.SessionDescriptor;

/**
 * SessionDescriptor representing a session by the user's SubjectIdentity.
 */
public class SubjectDescriptor implements SessionDescriptor {

  private final SubjectIdentifier identifier;

  private SubjectDescriptor(SubjectIdentifier identifier) {
    this.identifier = identifier;
  }

  public SubjectIdentifier getIdentifier() {
    return identifier;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private SubjectIdentifier identifier;

    private Builder() {
    }

    public SubjectDescriptor build() {
      return new SubjectDescriptor(identifier);
    }

    public Builder setIdentifier(SubjectIdentifier identifier) {
      this.identifier = identifier;
      return this;
    }
  }
}
