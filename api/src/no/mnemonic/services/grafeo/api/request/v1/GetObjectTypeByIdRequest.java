package no.mnemonic.services.grafeo.api.request.v1;

import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class GetObjectTypeByIdRequest implements ValidatingRequest {

  @NotNull
  private UUID id;

  public UUID getId() {
    return id;
  }

  public GetObjectTypeByIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

}
