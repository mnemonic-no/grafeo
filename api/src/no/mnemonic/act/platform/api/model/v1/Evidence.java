package no.mnemonic.act.platform.api.model.v1;

public class Evidence {

  private final String checksum;
  // Keep it simple for now and just return the data as stored in the database
  // We might want to do something similar as the case attachments in the future.
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
