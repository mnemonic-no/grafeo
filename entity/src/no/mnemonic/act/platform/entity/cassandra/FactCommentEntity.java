package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.*;
import static no.mnemonic.act.platform.entity.cassandra.FactCommentEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class FactCommentEntity implements CassandraEntity {

  public static final String TABLE = "fact_comment";

  @PartitionKey
  @Column(name = "fact_id")
  private UUID factID;
  @ClusteringColumn
  private UUID id;
  @Column(name = "reply_to_id")
  private UUID replyToID;
  @Column(name = "source_id")
  private UUID sourceID;
  private String comment;
  private long timestamp;

  public UUID getFactID() {
    return factID;
  }

  public FactCommentEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }

  public UUID getId() {
    return id;
  }

  public FactCommentEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getReplyToID() {
    return replyToID;
  }

  public FactCommentEntity setReplyToID(UUID replyToID) {
    this.replyToID = replyToID;
    return this;
  }

  public UUID getSourceID() {
    return sourceID;
  }

  public FactCommentEntity setSourceID(UUID sourceID) {
    this.sourceID = sourceID;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public FactCommentEntity setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactCommentEntity setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

}
