package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.*;
import static no.mnemonic.act.platform.entity.cassandra.ObjectEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class ObjectEntity implements CassandraEntity {

  public static final String TABLE = "object";

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
