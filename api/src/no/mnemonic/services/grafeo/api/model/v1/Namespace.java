package no.mnemonic.services.grafeo.api.model.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Namespaces distinguish meta data imported from different system instances. " +
        "For example, namespaces resolve conflicts between same types defined in multiple instances."
)
public class Namespace {

  @Schema(description = "Uniquely identifies the Namespace", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Name of the Namespace", example = "mnemonic", requiredMode = REQUIRED)
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
