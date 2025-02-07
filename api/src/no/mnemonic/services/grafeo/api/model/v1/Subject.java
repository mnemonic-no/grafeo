package no.mnemonic.services.grafeo.api.model.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Represents a Subject inside the system, e.g. a user.")
public class Subject {

  @Schema(description = "Uniquely identifies the Subject", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Name of the Subject", example = "John Doe", requiredMode = REQUIRED)
  private final String name;
  @Schema(description = "Organization the Subject belongs to")
  private final Organization.Info organization;

  private Subject(UUID id, String name, Organization.Info organization) {
    this.id = id;
    this.name = name;
    this.organization = organization;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Organization.Info getOrganization() {
    return organization;
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
    private Organization.Info organization;

    private Builder() {
    }

    public Subject build() {
      return new Subject(id, name, organization);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setOrganization(Organization.Info organization) {
      this.organization = organization;
      return this;
    }
  }

  @Schema(name = "SubjectInfo", description = "Short summary of a Subject.")
  public static class Info {
    @Schema(description = "Uniquely identifies the Subject", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
    private final UUID id;
    @Schema(description = "Name of the Subject", example = "John Doe", requiredMode = REQUIRED)
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
