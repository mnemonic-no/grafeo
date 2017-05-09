package no.mnemonic.act.platform.auth.properties.model;

import no.mnemonic.services.common.auth.model.FunctionIdentity;

/**
 * Identifies a Function by its unique name.
 */
public class FunctionIdentifier implements FunctionIdentity {

  private final String name;

  private FunctionIdentifier(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;

    private Builder() {
    }

    public FunctionIdentifier build() {
      return new FunctionIdentifier(name);
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }
  }
}
