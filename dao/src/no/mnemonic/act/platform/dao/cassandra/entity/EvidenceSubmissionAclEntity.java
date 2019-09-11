package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.EvidenceSubmissionAclEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class EvidenceSubmissionAclEntity implements CassandraEntity {

  public static final String TABLE = "evidence_submission_acl";

  @PartitionKey
  @CqlName("submission_id")
  private UUID submissionID;
  @ClusteringColumn
  private UUID id;
  @CqlName("subject_id")
  private UUID subjectID;
  @CqlName("source_id")
  private UUID originID;
  private long timestamp;

  public UUID getSubmissionID() {
    return submissionID;
  }

  public EvidenceSubmissionAclEntity setSubmissionID(UUID submissionID) {
    this.submissionID = submissionID;
    return this;
  }

  public UUID getId() {
    return id;
  }

  public EvidenceSubmissionAclEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getSubjectID() {
    return subjectID;
  }

  public EvidenceSubmissionAclEntity setSubjectID(UUID subjectID) {
    this.subjectID = subjectID;
    return this;
  }

  public UUID getOriginID() {
    return originID;
  }

  public EvidenceSubmissionAclEntity setOriginID(UUID originID) {
    this.originID = originID;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public EvidenceSubmissionAclEntity setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

}
