package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(
        keyspace = "act",
        name = "object_by_type_value",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class ObjectByTypeValueEntity implements CassandraEntity {

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
