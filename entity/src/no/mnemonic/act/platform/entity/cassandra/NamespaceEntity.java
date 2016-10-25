package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(
        keyspace = "act",
        name = "namespace",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class NamespaceEntity implements CassandraEntity {

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
