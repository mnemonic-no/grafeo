package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.NotBlank;

public class GetObjectByTypeValueRequest implements ValidatingRequest {

  @NotBlank
  private String type;
  @NotBlank
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
