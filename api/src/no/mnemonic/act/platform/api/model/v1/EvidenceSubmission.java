package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampSerializer;

import java.util.UUID;

@ApiModel(description = "EvidenceSubmission contains meta data about one submitted evidence. " +
        "Multiple submissions can link to the same evidence data.")
public class EvidenceSubmission {

  @ApiModelProperty(value = "Uniquely identifies the submission", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Name of the submission", example = "APT1 report", required = true)
  private final String name;
  @ApiModelProperty(value = "TBD: What is the difference between 'dataType' and 'mediaType'?")
  private final String dataType;
  @ApiModelProperty(value = "Media type of the evidence as defined in RFC2046 and standardized by IANA", example = "application/pdf", required = true)
  private final String mediaType;
  @ApiModelProperty(value = "Length of the evidence data in bytes", example = "723298", required = true)
  private final long length;
  @ApiModelProperty(value = "When the evidence was submitted", example = "2016-09-28T21:26:22Z", dataType = "string", required = true)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;
  @ApiModelProperty(value = "When the evidence was first observed", example = "2016-09-28T21:26:22Z", dataType = "string", required = true)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long observationTimestamp;
  @ApiModelProperty(value = "Who submitted the evidence", required = true)
  private final Origin.Info origin;
  @ApiModelProperty(value = "Who has access to the evidence", required = true)
  private final AccessMode accessMode;
  @ApiModelProperty(value = "Checksum of the evidence data using SHA-256",
          example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", required = true)
  private final String checksum;

  private EvidenceSubmission(UUID id, String name, String dataType, String mediaType, long length, Long timestamp,
                             Long observationTimestamp, Origin.Info origin, AccessMode accessMode, String checksum) {
    this.id = id;
    this.name = name;
    this.dataType = dataType;
    this.mediaType = mediaType;
    this.length = length;
    this.timestamp = timestamp;
    this.observationTimestamp = observationTimestamp;
    this.origin = origin;
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

  public Long getTimestamp() {
    return timestamp;
  }

  public Long getObservationTimestamp() {
    return observationTimestamp;
  }

  public Origin.Info getOrigin() {
    return origin;
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
    private Long timestamp;
    private Long observationTimestamp;
    private Origin.Info origin;
    private AccessMode accessMode;
    private String checksum;

    private Builder() {
    }

    public EvidenceSubmission build() {
      return new EvidenceSubmission(id, name, dataType, mediaType, length, timestamp, observationTimestamp, origin, accessMode, checksum);
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

    public Builder setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder setObservationTimestamp(Long observationTimestamp) {
      this.observationTimestamp = observationTimestamp;
      return this;
    }

    public Builder setOrigin(Origin.Info origin) {
      this.origin = origin;
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
