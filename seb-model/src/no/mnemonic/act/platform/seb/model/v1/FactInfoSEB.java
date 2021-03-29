package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.UUID;

@JsonDeserialize(builder = FactInfoSEB.Builder.class)
public class FactInfoSEB {

  private final UUID id;
  private final FactTypeInfoSEB type;
  private final String value;

  private FactInfoSEB(UUID id, FactTypeInfoSEB type, String value) {
    this.id = id;
    this.type = type;
    this.value = value;
  }

  public UUID getId() {
    return id;
  }

  public FactTypeInfoSEB getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "set")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Builder {
    private UUID id;
    private FactTypeInfoSEB type;
    private String value;

    private Builder() {
    }

    public FactInfoSEB build() {
      return new FactInfoSEB(id, type, value);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setType(FactTypeInfoSEB type) {
      this.type = type;
      return this;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }
  }
}
