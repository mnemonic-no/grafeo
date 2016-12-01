package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.mnemonic.act.platform.api.json.TimestampSerializer;

import java.util.UUID;

public class AclEntry {

  private final UUID id;
  private final Subject.Info subject;
  private final Source.Info source;
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;

  private AclEntry(UUID id, Subject.Info subject, Source.Info source, Long timestamp) {
    this.id = id;
    this.subject = subject;
    this.source = source;
    this.timestamp = timestamp;
  }

  public UUID getId() {
    return id;
  }

  public Subject.Info getSubject() {
    return subject;
  }

  public Source.Info getSource() {
    return source;
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
    private Source.Info source;
    private Long timestamp;

    private Builder() {
    }

    public AclEntry build() {
      return new AclEntry(id, subject, source, timestamp);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setSubject(Subject.Info subject) {
      this.subject = subject;
      return this;
    }

    public Builder setSource(Source.Info source) {
      this.source = source;
      return this;
    }

    public Builder setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }
  }

}
