package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class GetFactAclRequest {

  @NotNull
  private UUID fact;

  public UUID getFact() {
    return fact;
  }

  public GetFactAclRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

}
