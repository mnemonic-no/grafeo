package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.*;
import static no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class ObjectFactBindingEntity implements CassandraEntity {

  public static final String TABLE = "object_fact_binding";

  @PartitionKey
  @Column(name = "object_id")
  private UUID objectID;
  @ClusteringColumn
  @Column(name = "fact_id")
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
