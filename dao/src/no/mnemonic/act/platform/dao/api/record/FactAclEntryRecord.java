package no.mnemonic.act.platform.dao.api.record;

import java.util.UUID;

/**
 * Record representing an entry in a Fact's ACL.
 */
public class FactAclEntryRecord {

  private UUID id;
  private UUID subjectID;
  private UUID originID;
  private long timestamp;

  public UUID getId() {
    return id;
  }

  public FactAclEntryRecord setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getSubjectID() {
    return subjectID;
  }

  public FactAclEntryRecord setSubjectID(UUID subjectID) {
    this.subjectID = subjectID;
    return this;
  }

  public UUID getOriginID() {
    return originID;
  }

  public FactAclEntryRecord setOriginID(UUID originID) {
    this.originID = originID;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactAclEntryRecord setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }
}
