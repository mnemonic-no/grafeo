package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.mnemonic.act.platform.api.json.TimestampSerializer;

import java.util.UUID;

public class FactComment {

  private final UUID id;
  private final UUID replyTo;
  private final Source.Info source;
  private final String comment;
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;

  private FactComment(UUID id, UUID replyTo, Source.Info source, String comment, Long timestamp) {
    this.id = id;
    this.replyTo = replyTo;
    this.source = source;
    this.comment = comment;
    this.timestamp = timestamp;
  }

  public UUID getId() {
    return id;
  }

  public UUID getReplyTo() {
    return replyTo;
  }

  public Source.Info getSource() {
    return source;
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
    private Source.Info source;
    private String comment;
    private Long timestamp;

    private Builder() {
    }

    public FactComment build() {
      return new FactComment(id, replyTo, source, comment, timestamp);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setReplyTo(UUID replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public Builder setSource(Source.Info source) {
      this.source = source;
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
