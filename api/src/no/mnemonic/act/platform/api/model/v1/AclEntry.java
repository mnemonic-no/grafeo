package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.utilities.json.TimestampSerializer;

import java.util.UUID;

@ApiModel(description = "An entry inside an Access Control List.")
public class AclEntry {

  @ApiModelProperty(value = "Uniquely identifies the ACL entry", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "To whom access is granted", required = true)
  private final Subject.Info subject;
  @ApiModelProperty(value = "Who granted access", required = true)
  private final Origin.Info origin;
  @ApiModelProperty(value = "When access was granted", example = "2016-09-28T21:26:22Z", dataType = "string", required = true)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;

  private AclEntry(UUID id, Subject.Info subject, Origin.Info origin, Long timestamp) {
    this.id = id;
    this.subject = subject;
    this.origin = origin;
    this.timestamp = timestamp;
  }

  public UUID getId() {
    return id;
  }

  public Subject.Info getSubject() {
    return subject;
  }

  public Origin.Info getOrigin() {
    return origin;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private Subject.Info subject;
    private Origin.Info origin;
    private Long timestamp;

    private Builder() {
    }

    public AclEntry build() {
      return new AclEntry(id, subject, origin, timestamp);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setSubject(Subject.Info subject) {
      this.subject = subject;
      return this;
    }

    public Builder setOrigin(Origin.Info origin) {
      this.origin = origin;
      return this;
    }

    public Builder setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }
  }

}
