package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@ApiModel(value = "FactObjectBindingDefinitionRequest", description = "Define to which Objects new Facts of a particular type can be linked.")
public class FactObjectBindingDefinition {

  @ApiModelProperty(value = "Type linked Objects must have (takes ObjectType UUID)", required = true)
  @NotNull
  private UUID objectType;
  @ApiModelProperty(value = "Direction the link must have", required = true)
  @NotNull
  private Direction direction;

  public UUID getObjectType() {
    return objectType;
  }

  public FactObjectBindingDefinition setObjectType(UUID objectType) {
    this.objectType = objectType;
    return this;
  }

  public Direction getDirection() {
    return direction;
  }

  public FactObjectBindingDefinition setDirection(Direction direction) {
    this.direction = direction;
    return this;
  }

}
