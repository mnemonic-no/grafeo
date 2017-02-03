package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class GetObjectByIdRequest implements ValidatingRequest {

  @NotNull
  private UUID id;

  public UUID getId() {
    return id;
  }

  public GetObjectByIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

}
