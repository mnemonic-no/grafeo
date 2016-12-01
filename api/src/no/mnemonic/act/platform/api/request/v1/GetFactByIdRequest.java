package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class GetFactByIdRequest {

  @NotNull
  private UUID id;

  public UUID getId() {
    return id;
  }

  public GetFactByIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

}
