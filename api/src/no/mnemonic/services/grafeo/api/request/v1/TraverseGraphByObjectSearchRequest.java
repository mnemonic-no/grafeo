package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Traverse the Object/Fact graph starting at the Objects returned from an Object search.")
public class TraverseGraphByObjectSearchRequest implements ValidatingRequest {

  @Schema(description = "Object search to execute", requiredMode = REQUIRED)
  @Valid
  @NotNull
  private SearchObjectRequest search;

  @Schema(description = "Traversal from objects in the search result", requiredMode = REQUIRED)
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
