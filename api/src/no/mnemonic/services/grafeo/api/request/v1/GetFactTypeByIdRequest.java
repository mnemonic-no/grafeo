package no.mnemonic.services.grafeo.api.request.v1;

import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class GetFactTypeByIdRequest implements ValidatingRequest {

  @NotNull
  private UUID id;

  public UUID getId() {
    return id;
  }

  public GetFactTypeByIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

}
