package no.mnemonic.act.platform.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

@ApiModel(description = "An ObjectType defines how an Object will be handled inside the system.")
public class ObjectType {

  @ApiModelProperty(value = "Uniquely identifies the ObjectType", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Namespace the ObjectType belongs to", required = true)
  private final Namespace namespace;
  @ApiModelProperty(value = "Name of the ObjectType. Unique per Namespace", example = "ip", required = true)
  private final String name;
  @ApiModelProperty(value = "Validator used to validate new Objects of this type", example = "RegexValidator", required = true)
  private final String validator;
  @ApiModelProperty(value = "Parameters used to customize Validator", example = "(\\d+).(\\d+).(\\d+).(\\d+)")
  private final String validatorParameter;
  @ApiModelProperty(value = "EntityHandler used to store new Objects of this type", example = "IpEntityHandler", required = true)
  private final String entityHandler;
  @ApiModelProperty(value = "Parameters used to customize EntityHandler")
  private final String entityHandlerParameter;

  private ObjectType(UUID id, Namespace namespace, String name, String validator, String validatorParameter, String entityHandler, String entityHandlerParameter) {
    this.id = id;
    this.namespace = namespace;
    this.name = name;
    this.validator = validator;
    this.validatorParameter = validatorParameter;
    this.entityHandler = entityHandler;
    this.entityHandlerParameter = entityHandlerParameter;
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

  public String getEntityHandler() {
    return entityHandler;
  }

  public String getEntityHandlerParameter() {
    return entityHandlerParameter;
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
    private String entityHandler;
    private String entityHandlerParameter;

    private Builder() {
    }

    public ObjectType build() {
      return new ObjectType(id, namespace, name, validator, validatorParameter, entityHandler, entityHandlerParameter);
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

    public Builder setEntityHandler(String entityHandler) {
      this.entityHandler = entityHandler;
      return this;
    }

    public Builder setEntityHandlerParameter(String entityHandlerParameter) {
      this.entityHandlerParameter = entityHandlerParameter;
      return this;
    }
  }

  @ApiModel(value = "ObjectTypeInfo", description = "Short summary of an ObjectType.")
  public class Info {
    @ApiModelProperty(value = "Uniquely identifies the ObjectType", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
    private final UUID id;
    @ApiModelProperty(value = "Name of the ObjectType", example = "IP", required = true)
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
