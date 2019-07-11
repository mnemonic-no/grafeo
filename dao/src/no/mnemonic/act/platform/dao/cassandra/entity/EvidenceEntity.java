package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.EvidenceEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class EvidenceEntity implements CassandraEntity {

  public static final String TABLE = "evidence";

  @PartitionKey
  private String checksum;
  // Store data as a string first, because the Cassandra driver maps BLOB to a ByteBuffer.
  // Keep it simple in the beginning, optimize later.
  private String data;

  public String getChecksum() {
    return checksum;
  }

  public EvidenceEntity setChecksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  public String getData() {
    return data;
  }

  public EvidenceEntity setData(String data) {
    this.data = data;
    return this;
  }

}
