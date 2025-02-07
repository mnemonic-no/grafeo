package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.utilities.json.TimestampSerializer;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Comment added to a Fact.")
public class FactComment {

  @Schema(description = "Uniquely identifies the comment", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Links to another comment to which this comment is a reply", example = "123e4567-e89b-12d3-a456-426655440000")
  private final UUID replyTo;
  @Schema(description = "Who made the comment", requiredMode = REQUIRED)
  private final Origin.Info origin;
  @Schema(description = "Contains the content of the comment", example = "Hello World!", requiredMode = REQUIRED)
  private final String comment;
  @Schema(description = "When the comment was made", example = "2016-09-28T21:26:22Z", type = "string", requiredMode = REQUIRED)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;

  private FactComment(UUID id, UUID replyTo, Origin.Info origin, String comment, Long timestamp) {
    this.id = id;
    this.replyTo = replyTo;
    this.origin = origin;
    this.comment = comment;
    this.timestamp = timestamp;
  }

  public UUID getId() {
    return id;
  }

  public UUID getReplyTo() {
    return replyTo;
  }

  public Origin.Info getOrigin() {
    return origin;
  }

  public String getComment() {
    return comment;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private UUID replyTo;
    private Origin.Info origin;
    private String comment;
    private Long timestamp;

    private Builder() {
    }

    public FactComment build() {
      return new FactComment(id, replyTo, origin, comment, timestamp);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setReplyTo(UUID replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public Builder setOrigin(Origin.Info origin) {
      this.origin = origin;
      return this;
    }

    public Builder setComment(String comment) {
      this.comment = comment;
      return this;
    }

    public Builder setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }
  }

}
