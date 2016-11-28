package no.mnemonic.act.platform.api.model.v1;

import java.util.UUID;

public class Namespace {

  private final UUID id;
  private final String name;

  private Namespace(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private String name;

    private Builder() {
    }

    public Namespace build() {
      return new Namespace(id, name);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }
  }

}
