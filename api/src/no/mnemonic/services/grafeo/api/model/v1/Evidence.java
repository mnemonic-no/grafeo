package no.mnemonic.services.grafeo.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Actual evidence data corresponding to one or more EvidenceSubmissions.")
public class Evidence {

  @ApiModelProperty(value = "Checksum of the evidence data using SHA-256. Uniquely identifies the evidence",
          example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", required = true)
  private final String checksum;
  // Keep it simple for now and just return the data as stored in the database
  // We might want to do something similar as the case attachments in the future.
  @ApiModelProperty(value = "Actual evidence data formatted using the media type of the corresponding EvidenceSubmission", required = true)
  private final String data;

  private Evidence(String checksum, String data) {
    this.checksum = checksum;
    this.data = data;
  }

  public String getChecksum() {
    return checksum;
  }

  public String getData() {
    return data;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String checksum;
    private String data;

    private Builder() {
    }

    public Evidence build() {
      return new Evidence(checksum, data);
    }

    public Builder setChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public Builder setData(String data) {
      this.data = data;
      return this;
    }
  }

}
