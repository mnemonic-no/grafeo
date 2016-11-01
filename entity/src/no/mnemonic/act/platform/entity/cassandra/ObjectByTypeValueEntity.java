package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.*;
import static no.mnemonic.act.platform.entity.cassandra.ObjectByTypeValueEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class ObjectByTypeValueEntity implements CassandraEntity {

  public static final String TABLE = "object_by_type_value";

  @PartitionKey(0)
  @Column(name = "object_type_id")
  private UUID objectTypeID;
  @PartitionKey(1)
  @Column(name = "object_value")
  private String objectValue;
  @Column(name = "object_id")
  private UUID objectID;

  public UUID getObjectTypeID() {
    return objectTypeID;
  }

  public ObjectByTypeValueEntity setObjectTypeID(UUID objectTypeID) {
    this.objectTypeID = objectTypeID;
    return this;
  }

  public String getObjectValue() {
    return objectValue;
  }

  public ObjectByTypeValueEntity setObjectValue(String objectValue) {
    this.objectValue = objectValue;
    return this;
  }

  public UUID getObjectID() {
    return objectID;
  }

  public ObjectByTypeValueEntity setObjectID(UUID objectID) {
    this.objectID = objectID;
    return this;
  }

}
