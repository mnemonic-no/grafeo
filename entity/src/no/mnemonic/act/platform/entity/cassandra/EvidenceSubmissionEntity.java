package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static no.mnemonic.act.platform.entity.cassandra.CassandraEntity.*;
import static no.mnemonic.act.platform.entity.cassandra.EvidenceSubmissionEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class EvidenceSubmissionEntity implements CassandraEntity {

  public static final String TABLE = "evidence_submission";

  @PartitionKey
  private UUID id;
  private String name;
  @Column(name = "data_type")
  private String dataType;
  @Column(name = "media_type")
  private String mediaType;
  private long length;
  private long timestamp;
  @Column(name = "observation_timestamp")
  private long observationTimestamp;
  @Column(name = "source_id")
  private UUID sourceID;
  @Column(name = "access_mode")
  private AccessMode accessMode;
  private String checksum;

  public UUID getId() {
    return id;
  }

  public EvidenceSubmissionEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public EvidenceSubmissionEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getDataType() {
    return dataType;
  }

  public EvidenceSubmissionEntity setDataType(String dataType) {
    this.dataType = dataType;
    return this;
  }

  public String getMediaType() {
    return mediaType;
  }

  public EvidenceSubmissionEntity setMediaType(String mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public long getLength() {
    return length;
  }

  public EvidenceSubmissionEntity setLength(long length) {
    this.length = length;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public EvidenceSubmissionEntity setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public long getObservationTimestamp() {
    return observationTimestamp;
  }

  public EvidenceSubmissionEntity setObservationTimestamp(long observationTimestamp) {
    this.observationTimestamp = observationTimestamp;
    return this;
  }

  public UUID getSourceID() {
    return sourceID;
  }

  public EvidenceSubmissionEntity setSourceID(UUID sourceID) {
    this.sourceID = sourceID;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public EvidenceSubmissionEntity setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public String getChecksum() {
    return checksum;
  }

  public EvidenceSubmissionEntity setChecksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

}
