package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "act",
        name = "evidence",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class EvidenceEntity implements CassandraEntity {

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
