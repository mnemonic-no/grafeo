package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import javax.validation.constraints.NotBlank;

@ApiModel(description = "Traverse the Object/Fact graph starting at an Object identified by its type and value.")
public class TraverseByObjectTypeValueRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private String type;

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private String value;

  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()", required = true)
  @NotBlank
  private String query;

  public String getType() {
    return type;
  }

  public TraverseByObjectTypeValueRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getValue() {
    return value;
  }

  public TraverseByObjectTypeValueRequest setValue(String value) {
    this.value = value;
    return this;
  }

  public String getQuery() {
    return query;
  }

  public TraverseByObjectTypeValueRequest setQuery(String query) {
    this.query = query;
    return this;
  }

}
