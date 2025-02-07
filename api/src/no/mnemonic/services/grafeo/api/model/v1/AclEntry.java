package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.utilities.json.TimestampSerializer;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "An entry inside an Access Control List.")
public class AclEntry {

  @Schema(description = "Uniquely identifies the ACL entry", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "To whom access is granted", requiredMode = REQUIRED)
  private final Subject.Info subject;
  @Schema(description = "Who granted access", requiredMode = REQUIRED)
  private final Origin.Info origin;
  @Schema(description = "When access was granted", example = "2016-09-28T21:26:22Z", type = "string", requiredMode = REQUIRED)
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
