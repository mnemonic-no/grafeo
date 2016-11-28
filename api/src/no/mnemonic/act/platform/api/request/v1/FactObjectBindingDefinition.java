package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class FactObjectBindingDefinition {

  @NotNull
  private UUID objectType;
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
