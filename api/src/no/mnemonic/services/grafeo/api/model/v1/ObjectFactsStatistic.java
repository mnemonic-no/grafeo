package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.utilities.json.TimestampSerializer;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Statistics about Facts of a specific type bound to one Object. Statistics are collected per FactType.")
public class ObjectFactsStatistic {

  @Schema(description = "Describes for which FactType the statistics are collected", requiredMode = REQUIRED)
  private final FactType.Info type;
  @Schema(description = "Number of Facts of a specific FactType bound to the Object", example = "127", requiredMode = REQUIRED)
  private final int count;
  @Schema(description = "Timestamp when a Fact of a specific FactType was last added to the Object",
          example = "2016-09-28T21:26:22Z", type = "string")
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long lastAddedTimestamp;
  @Schema(description = "Timestamp when a Fact of a specific FactType was last seen",
          example = "2016-09-28T21:26:22Z", type = "string")
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long lastSeenTimestamp;

  private ObjectFactsStatistic(FactType.Info type, int count, Long lastAddedTimestamp, Long lastSeenTimestamp) {
    this.type = type;
    this.count = count;
    this.lastAddedTimestamp = lastAddedTimestamp;
    this.lastSeenTimestamp = lastSeenTimestamp;
  }

  public FactType.Info getType() {
    return type;
  }

  public int getCount() {
    return count;
  }

  public Long getLastAddedTimestamp() {
    return lastAddedTimestamp;
  }

  public Long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactType.Info type;
    private int count;
    private Long lastAddedTimestamp;
    private Long lastSeenTimestamp;

    private Builder() {
    }

    public ObjectFactsStatistic build() {
      return new ObjectFactsStatistic(type, count, lastAddedTimestamp, lastSeenTimestamp);
    }

    public Builder setType(FactType.Info type) {
      this.type = type;
      return this;
    }

    public Builder setCount(int count) {
      this.count = count;
      return this;
    }

    public Builder setLastAddedTimestamp(Long lastAddedTimestamp) {
      this.lastAddedTimestamp = lastAddedTimestamp;
      return this;
    }

    public Builder setLastSeenTimestamp(Long lastSeenTimestamp) {
      this.lastSeenTimestamp = lastSeenTimestamp;
      return this;
    }
  }

}
