package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@ApiModel(description = "Traverse the Object/Fact graph starting at the Objects returned from an Object search.")
public class TraverseByObjectSearchRequest extends SearchObjectRequest {

  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()")
  @NotNull
  @Size(min = 1)
  private String query;

  public String getQuery() {
    return query;
  }

  public TraverseByObjectSearchRequest setQuery(String query) {
    this.query = query;
    return this;
  }

}
