package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(
        keyspace = "act",
        name = "fact_acl",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class FactAclEntity implements CassandraEntity {

  @PartitionKey
  @Column(name = "fact_id")
  private UUID factID;
  @ClusteringColumn
  private UUID id;
  @Column(name = "subject_id")
  private UUID subjectID;
  @Column(name = "source_id")
  private UUID sourceID;
  private long timestamp;

  public UUID getFactID() {
    return factID;
  }

  public FactAclEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }

  public UUID getId() {
    return id;
  }

  public FactAclEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getSubjectID() {
    return subjectID;
  }

  public FactAclEntity setSubjectID(UUID subjectID) {
    this.subjectID = subjectID;
    return this;
  }

  public UUID getSourceID() {
    return sourceID;
  }

  public FactAclEntity setSourceID(UUID sourceID) {
    this.sourceID = sourceID;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactAclEntity setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

}
