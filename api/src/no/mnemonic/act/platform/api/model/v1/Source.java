package no.mnemonic.act.platform.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel(description = "Every piece of information (e.g. Facts) put into the system is marked with a Source " +
        "in order to define it's origin. A Source can be a user or an automatic (import) system."
)
public class Source {

  public enum Type {
    User, InputPort, AnalysisModule
  }

  @ApiModelProperty(value = "Uniquely identifies the Source", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Namespace the Source belongs to", required = true)
  private final Namespace namespace;
  @ApiModelProperty(value = "Organization the Source belongs to", required = true)
  private final Organization.Info organization;
  @ApiModelProperty(value = "Name of the Source", example = "AnalysisModuleXYZ", required = true)
  private final String name;
  @ApiModelProperty(value = "Type of the Source", required = true)
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

  @ApiModel(value = "SourceInfo", description = "Short summary of a Source.")
  public static class Info {
    @ApiModelProperty(value = "Uniquely identifies the Source", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
    private final UUID id;
    @ApiModelProperty(value = "Name of the Source", example = "AnalysisModuleXYZ", required = true)
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
