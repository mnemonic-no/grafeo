package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel(value = "FactObjectBindingDefinitionRequest", description = "Define to which Objects new Facts of a particular type can be linked.")
public class FactObjectBindingDefinition {

  @ApiModelProperty(value = "Specify the type any Object must have when linked as source to a Fact (takes ObjectType UUID)")
  private UUID sourceObjectType;
  @ApiModelProperty(value = "Specify the type any Object must have when linked as destination to a Fact (takes ObjectType UUID)")
  private UUID destinationObjectType;
  @ApiModelProperty(value = "Specify if the binding between source Object, Fact and destination Object must be bidirectional (default 'false')")
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
