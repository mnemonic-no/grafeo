package no.mnemonic.services.grafeo.api.model.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ObjectModel", description = "An Object represents a globally unique piece of information, e.g. an IP address or a domain.")
public class Object {

  @Schema(description = "Uniquely identifies the Object", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Type of the Object", requiredMode = REQUIRED)
  private final ObjectType.Info type;
  @Schema(description = "Contains the actual information", example = "27.13.4.125", requiredMode = REQUIRED)
  private final String value;
  @Schema(description = "Contains meta data about Facts bound to the Object")
  private final List<ObjectFactsStatistic> statistics;

  private Object(UUID id, ObjectType.Info type, String value, List<ObjectFactsStatistic> statistics) {
    this.id = id;
    this.type = type;
    this.value = value;
    this.statistics = ObjectUtils.ifNotNull(statistics, Collections::unmodifiableList);
  }

  public UUID getId() {
    return id;
  }

  public ObjectType.Info getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public List<ObjectFactsStatistic> getStatistics() {
    return statistics;
  }

  public Info toInfo() {
    return new Info(id, type, value);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private ObjectType.Info type;
    private String value;
    private List<ObjectFactsStatistic> statistics;

    private Builder() {
    }

    public Object build() {
      return new Object(id, type, value, statistics);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setType(ObjectType.Info type) {
      this.type = type;
      return this;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public Builder setStatistics(List<ObjectFactsStatistic> statistics) {
      this.statistics = ObjectUtils.ifNotNull(statistics, ListUtils::list);
      return this;
    }

    public Builder addStatistic(ObjectFactsStatistic statistic) {
      this.statistics = ListUtils.addToList(this.statistics, statistic);
      return this;
    }
  }

  @Schema(name = "ObjectInfo", description = "Short summary of an Object.")
  public static class Info {
    @Schema(description = "Uniquely identifies the Object", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
    private final UUID id;
    @Schema(description = "Type of the Object", requiredMode = REQUIRED)
    private final ObjectType.Info type;
    @Schema(description = "Contains the actual information", example = "27.13.4.125", requiredMode = REQUIRED)
    private final String value;

    private Info(UUID id, ObjectType.Info type, String value) {
      this.id = id;
      this.type = type;
      this.value = value;
    }

    public UUID getId() {
      return id;
    }

    public ObjectType.Info getType() {
      return type;
    }

    public String getValue() {
      return value;
    }
  }

}
