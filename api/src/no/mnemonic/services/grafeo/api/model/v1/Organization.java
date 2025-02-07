package no.mnemonic.services.grafeo.api.model.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Represents an Organization inside the system.")
public class Organization {

  @Schema(description = "Uniquely identifies the Organization", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Name of the Organization", example = "mnemonic", requiredMode = REQUIRED)
  private final String name;

  private Organization(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Info toInfo() {
    return new Info(id, name);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private String name;

    private Builder() {
    }

    public Organization build() {
      return new Organization(id, name);
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

  @Schema(name = "OrganizationInfo", description = "Short summary of an Organization.")
  public static class Info {
    @Schema(description = "Uniquely identifies the Organization", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
    private final UUID id;
    @Schema(description = "Name of the Organization", example = "mnemonic", requiredMode = REQUIRED)
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
