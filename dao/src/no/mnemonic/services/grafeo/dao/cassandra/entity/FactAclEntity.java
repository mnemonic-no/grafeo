package no.mnemonic.services.grafeo.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.services.grafeo.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.services.grafeo.dao.cassandra.entity.FactAclEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactAclEntity implements CassandraEntity {

  public static final String TABLE = "fact_acl";

  @PartitionKey
  @CqlName("fact_id")
  private UUID factID;
  @ClusteringColumn
  private UUID id;
  @CqlName("subject_id")
  private UUID subjectID;
  @CqlName("source_id")
  private UUID originID;
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

  public UUID getOriginID() {
    return originID;
  }

  public FactAclEntity setOriginID(UUID originID) {
    this.originID = originID;
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
