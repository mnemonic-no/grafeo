package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class GetObjectByIdRequest {

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
