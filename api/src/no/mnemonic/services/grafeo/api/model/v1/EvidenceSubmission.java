package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.utilities.json.TimestampSerializer;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "EvidenceSubmission contains meta data about one submitted evidence. " +
        "Multiple submissions can link to the same evidence data.")
public class EvidenceSubmission {

  @Schema(description = "Uniquely identifies the submission", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Name of the submission", example = "APT1 report", requiredMode = REQUIRED)
  private final String name;
  @Schema(description = "TBD: What is the difference between 'dataType' and 'mediaType'?")
  private final String dataType;
  @Schema(description = "Media type of the evidence as defined in RFC2046 and standardized by IANA", example = "application/pdf", requiredMode = REQUIRED)
  private final String mediaType;
  @Schema(description = "Length of the evidence data in bytes", example = "723298", requiredMode = REQUIRED)
  private final long length;
  @Schema(description = "When the evidence was submitted", example = "2016-09-28T21:26:22Z", type = "string", requiredMode = REQUIRED)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;
  @Schema(description = "When the evidence was first observed", example = "2016-09-28T21:26:22Z", type = "string", requiredMode = REQUIRED)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long observationTimestamp;
  @Schema(description = "Who submitted the evidence", requiredMode = REQUIRED)
  private final Origin.Info origin;
  @Schema(description = "Who has access to the evidence", requiredMode = REQUIRED)
  private final AccessMode accessMode;
  @Schema(description = "Checksum of the evidence data using SHA-256",
          example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", requiredMode = REQUIRED)
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
