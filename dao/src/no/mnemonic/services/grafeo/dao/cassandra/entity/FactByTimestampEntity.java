package no.mnemonic.services.grafeo.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.services.grafeo.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.services.grafeo.dao.cassandra.entity.FactByTimestampEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactByTimestampEntity implements CassandraEntity {

  public static final String TABLE = "fact_by_timestamp";

  @PartitionKey
  @CqlName("hour_of_day")
  private long hourOfDay;
  @ClusteringColumn(0)
  @CqlName("timestamp")
  private long timestamp;
  @ClusteringColumn(1)
  @CqlName("fact_id")
  private UUID factID;

  public long getHourOfDay() {
    return hourOfDay;
  }

  public FactByTimestampEntity setHourOfDay(long hourOfDay) {
    this.hourOfDay = hourOfDay;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactByTimestampEntity setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public UUID getFactID() {
    return factID;
  }

  public FactByTimestampEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }
}
