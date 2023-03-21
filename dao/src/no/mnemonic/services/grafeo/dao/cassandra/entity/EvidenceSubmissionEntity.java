package no.mnemonic.services.grafeo.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

import java.util.UUID;

import static no.mnemonic.services.grafeo.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.services.grafeo.dao.cassandra.entity.EvidenceSubmissionEntity.TABLE;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class EvidenceSubmissionEntity implements CassandraEntity {

  public static final String TABLE = "evidence_submission";

  @PartitionKey
  private UUID id;
  private String name;
  @CqlName("data_type")
  private String dataType;
  @CqlName("media_type")
  private String mediaType;
  private long length;
  private long timestamp;
  @CqlName("observation_timestamp")
  private long observationTimestamp;
  @CqlName("source_id")
  private UUID originID;
  @CqlName("access_mode")
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

  public UUID getOriginID() {
    return originID;
  }

  public EvidenceSubmissionEntity setOriginID(UUID originID) {
    this.originID = originID;
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
