package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.MetaFactBindingEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class MetaFactBindingEntity implements CassandraEntity {

  public static final String TABLE = "meta_fact_binding";

  @PartitionKey
  @CqlName("fact_id")
  private UUID factID;
  @ClusteringColumn
  @CqlName("meta_fact_id")
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
