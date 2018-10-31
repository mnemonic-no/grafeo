package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.*;
import static no.mnemonic.act.platform.dao.cassandra.entity.MetaFactBindingEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class MetaFactBindingEntity implements CassandraEntity {

  public static final String TABLE = "meta_fact_binding";

  @PartitionKey
  @Column(name = "fact_id")
  private UUID factID;
  @ClusteringColumn
  @Column(name = "meta_fact_id")
  private UUID metaFactID;

  public UUID getFactID() {
    return factID;
  }

  public MetaFactBindingEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }

  public UUID getMetaFactID() {
    return metaFactID;
  }

  public MetaFactBindingEntity setMetaFactID(UUID metaFactID) {
    this.metaFactID = metaFactID;
    return this;
  }
}
