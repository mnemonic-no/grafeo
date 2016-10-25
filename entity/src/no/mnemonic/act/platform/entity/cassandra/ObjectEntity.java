package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(
        keyspace = "act",
        name = "object",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class ObjectEntity implements CassandraEntity {

  @PartitionKey
  private UUID id;
  @Column(name = "type_id")
  private UUID typeID;
  private String value;

  public UUID getId() {
    return id;
  }

  public ObjectEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getTypeID() {
    return typeID;
  }

  public ObjectEntity setTypeID(UUID typeID) {
    this.typeID = typeID;
    return this;
  }

  public String getValue() {
    return value;
  }

  public ObjectEntity setValue(String value) {
    this.value = value;
    return this;
  }

}
