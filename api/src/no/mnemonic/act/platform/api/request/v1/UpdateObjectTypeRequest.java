package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@ApiModel(description = "Update an existing ObjectType.")
public class UpdateObjectTypeRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @NotNull
  private UUID id;
  @ApiModelProperty(value = "If set updates the name of the ObjectType", example = "ip")
  @Size(min = 1)
  private String name;

  public UUID getId() {
    return id;
  }

  public UpdateObjectTypeRequest setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public UpdateObjectTypeRequest setName(String name) {
    this.name = name;
    return this;
  }

}
