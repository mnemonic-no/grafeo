package no.mnemonic.services.grafeo.seb.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.UUID;

@JsonDeserialize(builder = ObjectInfoSEB.Builder.class)
public class ObjectInfoSEB {

  private final UUID id;
  private final ObjectTypeInfoSEB type;
  private final String value;

  private ObjectInfoSEB(UUID id, ObjectTypeInfoSEB type, String value) {
    this.id = id;
    this.type = type;
    this.value = value;
  }

  public UUID getId() {
    return id;
  }

  public ObjectTypeInfoSEB getType() {
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
    private ObjectTypeInfoSEB type;
    private String value;

    private Builder() {
    }

    public ObjectInfoSEB build() {
      return new ObjectInfoSEB(id, type, value);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setType(ObjectTypeInfoSEB type) {
      this.type = type;
      return this;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }
  }
}
