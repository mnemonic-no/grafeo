package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.utilities.json.TimestampDeserializer;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class GetObjectByIdRequest implements ValidatingRequest {

  @NotNull
  private UUID id;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;

  public UUID getId() {
    return id;
  }

  public GetObjectByIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public GetObjectByIdRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public GetObjectByIdRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

}
