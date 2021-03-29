package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.mnemonic.act.platform.utilities.json.TimestampDeserializer;
import no.mnemonic.act.platform.utilities.json.TimestampSerializer;

import java.util.UUID;

@JsonDeserialize(builder = AclEntrySEB.Builder.class)
public class AclEntrySEB {

  private final UUID id;
  private final SubjectInfoSEB subject;
  private final OriginInfoSEB origin;
  @JsonSerialize(using = TimestampSerializer.class)
  private final long timestamp;

  private AclEntrySEB(UUID id, SubjectInfoSEB subject, OriginInfoSEB origin, long timestamp) {
    this.id = id;
    this.subject = subject;
    this.origin = origin;
    this.timestamp = timestamp;
  }

  public UUID getId() {
    return id;
  }

  public SubjectInfoSEB getSubject() {
    return subject;
  }

  public OriginInfoSEB getOrigin() {
    return origin;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "set")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Builder {
    private UUID id;
    private SubjectInfoSEB subject;
    private OriginInfoSEB origin;
    @JsonDeserialize(using = TimestampDeserializer.class)
    private long timestamp;

    private Builder() {
    }

    public AclEntrySEB build() {
      return new AclEntrySEB(id, subject, origin, timestamp);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setSubject(SubjectInfoSEB subject) {
      this.subject = subject;
      return this;
    }

    public Builder setOrigin(OriginInfoSEB origin) {
      this.origin = origin;
      return this;
    }

    public Builder setTimestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }
  }
}
