package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(
        keyspace = "act",
        name = "evidence_submission_acl",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class EvidenceSubmissionAclEntity implements CassandraEntity {

  @PartitionKey
  @Column(name = "submission_id")
  private UUID submissionID;
  @ClusteringColumn
  private UUID id;
  @Column(name = "subject_id")
  private UUID subjectID;
  @Column(name = "source_id")
  private UUID sourceID;
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

  public UUID getSourceID() {
    return sourceID;
  }

  public EvidenceSubmissionAclEntity setSourceID(UUID sourceID) {
    this.sourceID = sourceID;
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
