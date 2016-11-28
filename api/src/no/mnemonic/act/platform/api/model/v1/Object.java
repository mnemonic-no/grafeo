package no.mnemonic.act.platform.api.model.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.List;
import java.util.UUID;

public class Object {

  private final UUID id;
  private final ObjectType.Info type;
  private final String value;
  private final List<ObjectFactsStatistic> statistics;

  private Object(UUID id, ObjectType.Info type, String value, List<ObjectFactsStatistic> statistics) {
    this.id = id;
    this.type = type;
    this.value = value;
    this.statistics = statistics;
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
      this.statistics = statistics;
      return this;
    }

    public Builder addStatistic(ObjectFactsStatistic statistic) {
      this.statistics = ListUtils.addToList(this.statistics, statistic);
      return this;
    }
  }

  public class Info {
    private final UUID id;
    private final ObjectType.Info type;
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
