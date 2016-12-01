package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class GrantFactAccessRequest {

  @NotNull
  private UUID fact;
  @NotNull
  private UUID subject;

  public UUID getFact() {
    return fact;
  }

  public GrantFactAccessRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public UUID getSubject() {
    return subject;
  }

  public GrantFactAccessRequest setSubject(UUID subject) {
    this.subject = subject;
    return this;
  }

}
