package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactRefreshLogEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactRefreshLogEntity implements CassandraEntity {

  public static final String TABLE = "fact_refresh_log";

  @PartitionKey
  @CqlName("fact_id")
  private UUID factID;
  @ClusteringColumn
  @CqlName("refreshed_timestamp")
  private long refreshTimestamp;
  @CqlName("refreshed_by_id")
  private UUID refreshedByID;

  public UUID getFactID() {
    return factID;
  }

  public FactRefreshLogEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }

  public long getRefreshTimestamp() {
    return refreshTimestamp;
  }

  public FactRefreshLogEntity setRefreshTimestamp(long refreshTimestamp) {
    this.refreshTimestamp = refreshTimestamp;
    return this;
  }

  public UUID getRefreshedByID() {
    return refreshedByID;
  }

  public FactRefreshLogEntity setRefreshedByID(UUID refreshedByID) {
    this.refreshedByID = refreshedByID;
    return this;
  }
}
