package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(
        keyspace = "act",
        name = "fact_evidence_binding",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class FactEvidenceBindingEntity implements CassandraEntity {

  @PartitionKey
  @Column(name = "fact_id")
  private UUID factID;
  @ClusteringColumn
  @Column(name = "submission_id")
  private UUID submissionID;

  public UUID getFactID() {
    return factID;
  }

  public FactEvidenceBindingEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }

  public UUID getSubmissionID() {
    return submissionID;
  }

  public FactEvidenceBindingEntity setSubmissionID(UUID submissionID) {
    this.submissionID = submissionID;
    return this;
  }

}
