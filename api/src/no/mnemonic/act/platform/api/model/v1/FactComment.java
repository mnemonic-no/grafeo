package no.mnemonic.act.platform.api.model.v1;

import java.util.UUID;

public class FactComment {

  private final UUID id;
  private final UUID replyTo;
  private final Source.Info source;
  private final String comment;
  private final String timestamp;

  private FactComment(UUID id, UUID replyTo, Source.Info source, String comment, String timestamp) {
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

  public String getTimestamp() {
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
    private String timestamp;

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

    public Builder setTimestamp(String timestamp) {
      this.timestamp = timestamp;
      return this;
    }
  }

}
