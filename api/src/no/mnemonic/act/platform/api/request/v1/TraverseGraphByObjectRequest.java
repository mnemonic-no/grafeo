package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.NotBlank;

@ApiModel(description = "Traverse the Object/Fact graph starting at an Object")
public class TraverseGraphByObjectRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Gremlin query to execute.", example = "g.out()", required = true)
  @NotBlank
  private String query;
  @ApiModelProperty(value = "Traverse retracted Facts (default false)", example = "false")
  private Boolean includeRetracted;
  @ApiModelProperty(value = "Only traverse Facts added before a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long before;
  @ApiModelProperty(value = "Only traverse Facts added after a specific timestamp",
          example = "2016-09-28T21:26:22Z", dataType = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  private Long after;

  public String getQuery() {
    return query;
  }

  public TraverseGraphByObjectRequest setQuery(String query) {
    this.query = query;
    return this;
  }

  public Boolean getIncludeRetracted() {
    return includeRetracted;
  }

  public TraverseGraphByObjectRequest setIncludeRetracted(Boolean includeRetracted) {
    this.includeRetracted = includeRetracted;
    return this;
  }

  public Long getBefore() {
    return before;
  }

  public TraverseGraphByObjectRequest setBefore(Long before) {
    this.before = before;
    return this;
  }

  public Long getAfter() {
    return after;
  }

  public TraverseGraphByObjectRequest setAfter(Long after) {
    this.after = after;
    return this;
  }
}
