package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class ObjectEntity implements CassandraEntity {

  public static final String TABLE = "object";

  @PartitionKey
  private UUID id;
  @CqlName("type_id")
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
