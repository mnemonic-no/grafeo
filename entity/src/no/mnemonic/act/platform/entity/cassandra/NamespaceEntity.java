package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.*;
import static no.mnemonic.act.platform.entity.cassandra.NamespaceEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
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
