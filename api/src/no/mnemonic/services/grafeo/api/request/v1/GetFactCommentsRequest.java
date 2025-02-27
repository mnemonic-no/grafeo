package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.utilities.json.TimestampDeserializer;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class GetFactCommentsRequest implements ValidatingRequest {

  @NotNull
  private UUID fact;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;

  public UUID getFact() {
    return fact;
  }

  public GetFactCommentsRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public GetFactCommentsRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public GetFactCommentsRequest setAfter(Long after) {
    this.after = after;
    return this;
  }

}
