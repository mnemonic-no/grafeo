package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.ObjectByTypeValueEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class ObjectByTypeValueEntity implements CassandraEntity {

  public static final String TABLE = "object_by_type_value";

  @PartitionKey(0)
  @CqlName("object_type_id")
  private UUID objectTypeID;
  @PartitionKey(1)
  @CqlName("object_value")
  private String objectValue;
  @CqlName("object_id")
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
