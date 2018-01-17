package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.*;
import static no.mnemonic.act.platform.dao.cassandra.entity.EvidenceEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
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
