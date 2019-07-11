package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactEvidenceBindingEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactEvidenceBindingEntity implements CassandraEntity {

  public static final String TABLE = "fact_evidence_binding";

  @PartitionKey
  @CqlName("fact_id")
  private UUID factID;
  @ClusteringColumn
  @CqlName("submission_id")
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
