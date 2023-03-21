package no.mnemonic.services.grafeo.dao.api.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Record representing a comment about a Fact.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FactCommentRecord {

  private UUID id;
  private UUID replyToID;
  private UUID originID;
  private String comment;
  private long timestamp;

  public UUID getId() {
    return id;
  }

  public FactCommentRecord setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getReplyToID() {
    return replyToID;
  }

  public FactCommentRecord setReplyToID(UUID replyToID) {
    this.replyToID = replyToID;
    return this;
  }

  public UUID getOriginID() {
    return originID;
  }

  public FactCommentRecord setOriginID(UUID originID) {
    this.originID = originID;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public FactCommentRecord setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactCommentRecord setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }
}
