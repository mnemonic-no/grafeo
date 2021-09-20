package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactExistenceEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactExistenceEntity implements CassandraEntity {

  public static final String TABLE = "fact_existence";

  @PartitionKey
  @CqlName("fact_hash")
  private String factHash;
  @CqlName("fact_id")
  private UUID factID;

  public String getFactHash() {
    return factHash;
  }

  public FactExistenceEntity setFactHash(String factHash) {
    this.factHash = factHash;
    return this;
  }

  public UUID getFactID() {
    return factID;
  }

  public FactExistenceEntity setFactID(UUID factID) {
    this.factID = factID;
    return this;
  }
}
