package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import java.util.UUID;

public class GrantFactAccessRequest implements ValidatingRequest {

  @ServiceNotNull
  private UUID fact;
  @ServiceNotNull
  private String subject;

  public UUID getFact() {
    return fact;
  }

  public GrantFactAccessRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public GrantFactAccessRequest setSubject(String subject) {
    this.subject = subject;
    return this;
  }

}
