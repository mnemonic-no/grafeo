package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactCommentEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactCommentEntity implements CassandraEntity {

  public static final String TABLE = "fact_comment";

  @PartitionKey
  @CqlName("fact_id")
  private UUID factID;
  @ClusteringColumn
  private UUID id;
  @CqlName("reply_to_id")
  private UUID replyToID;
  @CqlName("source_id")
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
