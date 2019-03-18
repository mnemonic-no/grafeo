package no.mnemonic.act.platform.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ApiModel(value = "ObjectModel", description = "An Object represents a globally unique piece of information, e.g. an IP address or a domain.")
public class Object {

  @ApiModelProperty(value = "Uniquely identifies the Object", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Type of the Object", required = true)
  private final ObjectType.Info type;
  @ApiModelProperty(value = "Contains the actual information", example = "27.13.4.125", required = true)
  private final String value;
  @ApiModelProperty(value = "Contains meta data about Facts bound to the Object")
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

  @ApiModel(value = "ObjectInfo", description = "Short summary of an Object.")
  public class Info {
    @ApiModelProperty(value = "Uniquely identifies the Object", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
    private final UUID id;
    @ApiModelProperty(value = "Type of the Object", required = true)
    private final ObjectType.Info type;
    @ApiModelProperty(value = "Contains the actual information", example = "27.13.4.125", required = true)
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
