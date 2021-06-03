package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.utilities.json.TimestampDeserializer;

import javax.validation.constraints.NotBlank;

public class GetObjectByTypeValueRequest implements ValidatingRequest {

  @NotBlank
  private String type;
  @NotBlank
  private String value;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;

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

  public Long getBefore() {
    return before;
  }

  public GetObjectByTypeValueRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public GetObjectByTypeValueRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

}
