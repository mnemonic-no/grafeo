package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@ApiModel(description = "Traverse the Object/Fact graph starting at the Objects returned from an Object search.")
public class TraverseGraphByObjectSearchRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Object search to execute", required = true)
  @Valid
  @NotNull
  private SearchObjectRequest search;

  @ApiModelProperty(value = "Traversal from objects in the search result", required = true)
  @Valid
  @NotNull
  private TraverseGraphRequest traverse;

  public SearchObjectRequest getSearch() {
    return search;
  }

  public TraverseGraphByObjectSearchRequest setSearch(SearchObjectRequest search) {
    this.search = search;
    return this;
  }

  public TraverseGraphRequest getTraverse() {
    return traverse;
  }

  public TraverseGraphByObjectSearchRequest setTraverse(TraverseGraphRequest traverse) {
    this.traverse = traverse;
    return this;
  }
}
