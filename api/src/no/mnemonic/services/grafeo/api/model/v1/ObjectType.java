package no.mnemonic.services.grafeo.api.model.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "An ObjectType defines how an Object will be handled inside the system.")
public class ObjectType {

  public enum IndexOption {
    Daily, TimeGlobal
  }

  @Schema(description = "Uniquely identifies the ObjectType", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Namespace the ObjectType belongs to", requiredMode = REQUIRED)
  private final Namespace namespace;
  @Schema(description = "Name of the ObjectType. Unique per Namespace", example = "ip", requiredMode = REQUIRED)
  private final String name;
  @Schema(description = "Validator used to validate new Objects of this type", example = "RegexValidator", requiredMode = REQUIRED)
  private final String validator;
  @Schema(description = "Parameters used to customize Validator", example = "(\\d+).(\\d+).(\\d+).(\\d+)")
  private final String validatorParameter;
  @Schema(description = "Specifies how Facts bound to Objects of this type will be indexed (default 'Daily')", example = "TimeGlobal", requiredMode = REQUIRED)
  private final IndexOption indexOption;

  private ObjectType(UUID id, Namespace namespace, String name, String validator, String validatorParameter, IndexOption indexOption) {
    this.id = id;
    this.namespace = namespace;
    this.name = name;
    this.validator = validator;
    this.validatorParameter = validatorParameter;
    this.indexOption = indexOption;
  }

  public UUID getId() {
    return id;
  }

  public Namespace getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public String getValidator() {
    return validator;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public IndexOption getIndexOption() {
    return indexOption;
  }

  public Info toInfo() {
    return new Info(id, name);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private Namespace namespace;
    private String name;
    private String validator;
    private String validatorParameter;
    private IndexOption indexOption;

    private Builder() {
    }

    public ObjectType build() {
      return new ObjectType(id, namespace, name, validator, validatorParameter, indexOption);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setNamespace(Namespace namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setValidator(String validator) {
      this.validator = validator;
      return this;
    }

    public Builder setValidatorParameter(String validatorParameter) {
      this.validatorParameter = validatorParameter;
      return this;
    }

    public Builder setIndexOption(IndexOption indexOption) {
      this.indexOption = indexOption;
      return this;
    }
  }

  @Schema(name = "ObjectTypeInfo", description = "Short summary of an ObjectType.")
  public static class Info {
    @Schema(description = "Uniquely identifies the ObjectType", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
    private final UUID id;
    @Schema(description = "Name of the ObjectType", example = "IP", requiredMode = REQUIRED)
    private final String name;

    private Info(UUID id, String name) {
      this.id = id;
      this.name = name;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }
}
