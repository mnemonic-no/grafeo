package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MetaFactBindingDefinitionRequest", description = "Define to which Facts new meta Facts of a particular type can be linked.")
public class MetaFactBindingDefinition {

  @Schema(description = "Specify the type any Fact must have when referenced by a new meta Fact (takes FactType UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  @NotNull
  private UUID factType;

  public UUID getFactType() {
    return factType;
  }

  public MetaFactBindingDefinition setFactType(UUID factType) {
    this.factType = factType;
    return this;
  }

}
