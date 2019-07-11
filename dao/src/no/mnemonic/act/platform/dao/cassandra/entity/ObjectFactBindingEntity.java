package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class ObjectFactBindingEntity implements CassandraEntity {

  public static final String TABLE = "object_fact_binding";

  @PartitionKey
  @CqlName("object_id")
  private UUID objectID;
  @ClusteringColumn
  @CqlName("fact_id")
  private UUID factID;
  private Direction direction;

  public UUID getObjectID() {
    return objectID;
  }

  public ObjectFactBindingEntity setObjectID(UUID objectID) {
    this.objectID = objectID;
    return this;
  }

  public UUID getFactID() {
    return factID;
  }

  public ObjectFactBindingEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }

  public Direction getDirection() {
    return direction;
  }

  public ObjectFactBindingEntity setDirection(Direction direction) {
    this.direction = direction;
    return this;
  }
}
