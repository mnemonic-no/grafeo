package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import javax.validation.constraints.NotBlank;
import java.util.UUID;

@ApiModel(description = "Traverse the Object/Fact graph starting at an Object identified by its ID.")
public class TraverseByObjectIdRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID id;

  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()", required = true)
  @NotBlank
  private String query;

  public UUID getId() {
    return id;
  }

  public TraverseByObjectIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getQuery() {
    return query;
  }

  public TraverseByObjectIdRequest setQuery(String query) {
    this.query = query;
    return this;
  }

}
