package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class GetObjectByTypeValueRequest implements ValidatingRequest {

  @NotNull
  @Size(min = 1)
  private String type;
  @NotNull
  @Size(min = 1)
  private String value;

  public String getType() {
    return type;
  }

  public GetObjectByTypeValueRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getValue() {
    return value;
  }

  public GetObjectByTypeValueRequest setValue(String value) {
    this.value = value;
    return this;
  }

}
