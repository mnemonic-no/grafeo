package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import javax.validation.constraints.NotBlank;
import java.util.UUID;

@ApiModel(description = "Traverse the Object/Fact graph starting at an Object identified by its ID.")
public class TraverseGraphByObjectIdRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID id;
  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()", required = true)
  @NotBlank
  private String query;
  @ApiModelProperty(value = "Include retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  @ApiModelProperty(value = "Only return Facts added before a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only return Facts added after a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;

  public UUID getId() {
    return id;
  }

  public TraverseGraphByObjectIdRequest setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getQuery() {
    return query;
  }

  public TraverseGraphByObjectIdRequest setQuery(String query) {
    this.query = query;
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public TraverseGraphByObjectIdRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public TraverseGraphByObjectIdRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public TraverseGraphByObjectIdRequest setAfter(Long after) {
    this.after = after;
    return this;
  }
}
