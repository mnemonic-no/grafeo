package no.mnemonic.services.grafeo.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel(description = "Namespaces distinguish meta data imported from different system instances. " +
        "For example, namespaces resolve conflicts between same types defined in multiple instances."
)
public class Namespace {

  @ApiModelProperty(value = "Uniquely identifies the Namespace", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Name of the Namespace", example = "mnemonic", required = true)
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
