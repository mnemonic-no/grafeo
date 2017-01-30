package no.mnemonic.act.platform.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel(description = "Represents a Subject inside the system, e.g. a user.")
public class Subject {

  @ApiModelProperty(value = "Uniquely identifies the Subject", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Name of the Subject", example = "John Doe", required = true)
  private final String name;

  private Subject(UUID id, String name) {
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

    public Subject build() {
      return new Subject(id, name);
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

  @ApiModel(value = "SubjectInfo", description = "Short summary of a Subject.")
  public class Info {
    @ApiModelProperty(value = "Uniquely identifies the Subject", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
    private final UUID id;
    @ApiModelProperty(value = "Name of the Subject", example = "John Doe", required = true)
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
