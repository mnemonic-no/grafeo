package no.mnemonic.services.grafeo.api.request.v1;

import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class GetOriginByIdRequest implements ValidatingRequest {

  @NotNull
  private UUID id;

  public UUID getId() {
    return id;
  }

  public GetOriginByIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

}
