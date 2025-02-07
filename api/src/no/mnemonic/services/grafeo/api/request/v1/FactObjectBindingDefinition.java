package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "FactObjectBindingDefinitionRequest", description = "Define to which Objects new Facts of a particular type can be linked.")
public class FactObjectBindingDefinition {

  @Schema(description = "Specify the type any Object must have when linked as source to a Fact (takes ObjectType UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID sourceObjectType;
  @Schema(description = "Specify the type any Object must have when linked as destination to a Fact (takes ObjectType UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID destinationObjectType;
  @Schema(description = "Specify if the binding between source Object, Fact and destination Object must be bidirectional (default 'false')")
  private boolean bidirectionalBinding;

  public UUID getSourceObjectType() {
    return sourceObjectType;
  }

  public FactObjectBindingDefinition setSourceObjectType(UUID sourceObjectType) {
    this.sourceObjectType = sourceObjectType;
    return this;
  }

  public UUID getDestinationObjectType() {
    return destinationObjectType;
  }

  public FactObjectBindingDefinition setDestinationObjectType(UUID destinationObjectType) {
    this.destinationObjectType = destinationObjectType;
    return this;
  }

  public boolean isBidirectionalBinding() {
    return bidirectionalBinding;
  }

  public FactObjectBindingDefinition setBidirectionalBinding(boolean bidirectionalBinding) {
    this.bidirectionalBinding = bidirectionalBinding;
    return this;
  }

}
