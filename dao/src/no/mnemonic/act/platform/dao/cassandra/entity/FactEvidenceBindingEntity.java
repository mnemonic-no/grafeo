package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.*;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactEvidenceBindingEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class FactEvidenceBindingEntity implements CassandraEntity {

  public static final String TABLE = "fact_evidence_binding";

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
