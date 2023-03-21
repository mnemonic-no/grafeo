package no.mnemonic.services.grafeo.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.services.grafeo.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.services.grafeo.dao.cassandra.entity.NamespaceEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class NamespaceEntity implements CassandraEntity {

  public static final String TABLE = "namespace";

  @PartitionKey
  private UUID id;
  private String name;

  public UUID getId() {
    return id;
  }

  public NamespaceEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public NamespaceEntity setName(String name) {
    this.name = name;
    return this;
  }

}
