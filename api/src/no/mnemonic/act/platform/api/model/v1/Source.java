package no.mnemonic.act.platform.api.model.v1;

import java.util.UUID;

public class Source {

  public enum Type {
    User, InputPort, AnalysisModule
  }

  private final UUID id;
  private final Namespace namespace;
  private final Organization.Info organization;
  private final String name;
  private final Type type;
  // TODO: Add trustLevel when it's defined.

  private Source(UUID id, Namespace namespace, Organization.Info organization, String name, Type type) {
    this.id = id;
    this.namespace = namespace;
    this.organization = organization;
    this.name = name;
    this.type = type;
  }

  public UUID getId() {
    return id;
  }

  public Namespace getNamespace() {
    return namespace;
  }

  public Organization.Info getOrganization() {
    return organization;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public Info toInfo() {
    return new Info(id, name);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private Namespace namespace;
    private Organization.Info organization;
    private String name;
    private Type type;

    private Builder() {
    }

    public Source build() {
      return new Source(id, namespace, organization, name, type);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setNamespace(Namespace namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder setOrganization(Organization.Info organization) {
      this.organization = organization;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setType(Type type) {
      this.type = type;
      return this;
    }
  }

  public class Info {
    private final UUID id;
    private final String name;

    private Info(UUID id, String name) {
      this.id = id;
      this.name = name;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

}
