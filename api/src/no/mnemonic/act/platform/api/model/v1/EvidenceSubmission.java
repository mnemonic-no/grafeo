package no.mnemonic.act.platform.api.model.v1;

import java.util.UUID;

public class EvidenceSubmission {

  private final UUID id;
  private final String name;
  private final String dataType;
  private final String mediaType;
  private final long length;
  private final String timestamp;
  private final String observationTimestamp;
  private final Source.Info source;
  private final AccessMode accessMode;
  private final String checksum;

  private EvidenceSubmission(UUID id, String name, String dataType, String mediaType, long length, String timestamp,
                             String observationTimestamp, Source.Info source, AccessMode accessMode, String checksum) {
    this.id = id;
    this.name = name;
    this.dataType = dataType;
    this.mediaType = mediaType;
    this.length = length;
    this.timestamp = timestamp;
    this.observationTimestamp = observationTimestamp;
    this.source = source;
    this.accessMode = accessMode;
    this.checksum = checksum;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDataType() {
    return dataType;
  }

  public String getMediaType() {
    return mediaType;
  }

  public long getLength() {
    return length;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getObservationTimestamp() {
    return observationTimestamp;
  }

  public Source.Info getSource() {
    return source;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public String getChecksum() {
    return checksum;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private String name;
    private String dataType;
    private String mediaType;
    private long length;
    private String timestamp;
    private String observationTimestamp;
    private Source.Info source;
    private AccessMode accessMode;
    private String checksum;

    private Builder() {
    }

    public EvidenceSubmission build() {
      return new EvidenceSubmission(id, name, dataType, mediaType, length, timestamp, observationTimestamp, source, accessMode, checksum);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setDataType(String dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder setMediaType(String mediaType) {
      this.mediaType = mediaType;
      return this;
    }

    public Builder setLength(long length) {
      this.length = length;
      return this;
    }

    public Builder setTimestamp(String timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder setObservationTimestamp(String observationTimestamp) {
      this.observationTimestamp = observationTimestamp;
      return this;
    }

    public Builder setSource(Source.Info source) {
      this.source = source;
      return this;
    }

    public Builder setAccessMode(AccessMode accessMode) {
      this.accessMode = accessMode;
      return this;
    }

    public Builder setChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }
  }

}
