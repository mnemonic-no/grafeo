package no.mnemonic.act.platform.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "A FactType defines how a Fact will be handled inside the system.")
public class FactType {

  @ApiModelProperty(value = "Uniquely identifies the FactType", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Namespace the FactType belongs to", required = true)
  private final Namespace namespace;
  @ApiModelProperty(value = "Name of the FactType. Unique per Namespace", example = "ThreatActorAlias", required = true)
  private final String name;
  @ApiModelProperty(value = "Validator used to validate new Facts of this type", example = "RegexValidator", required = true)
  private final String validator;
  @ApiModelProperty(value = "Parameters used to customize Validator", example = "(\\d+).(\\d+).(\\d+).(\\d+)")
  private final String validatorParameter;
  @ApiModelProperty(value = "EntityHandler used to store new Facts of this type", example = "StringEntityHandler", required = true)
  private final String entityHandler;
  @ApiModelProperty(value = "Parameters used to customize EntityHandler")
  private final String entityHandlerParameter;
  @ApiModelProperty(value = "Defines to which Objects new Facts of this type can be linked")
  private final List<FactObjectBindingDefinition> relevantObjectBindings;

  private FactType(UUID id, Namespace namespace, String name, String validator, String validatorParameter,
                   String entityHandler, String entityHandlerParameter, List<FactObjectBindingDefinition> relevantObjectBindings) {
    this.id = id;
    this.namespace = namespace;
    this.name = name;
    this.validator = validator;
    this.validatorParameter = validatorParameter;
    this.entityHandler = entityHandler;
    this.entityHandlerParameter = entityHandlerParameter;
    this.relevantObjectBindings = ObjectUtils.ifNotNull(relevantObjectBindings, Collections::unmodifiableList);
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

  public List<FactObjectBindingDefinition> getRelevantObjectBindings() {
    return relevantObjectBindings;
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
    private List<FactObjectBindingDefinition> relevantObjectBindings;

    private Builder() {
    }

    public FactType build() {
      return new FactType(id, namespace, name, validator, validatorParameter, entityHandler, entityHandlerParameter, relevantObjectBindings);
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

    public Builder setRelevantObjectBindings(List<FactObjectBindingDefinition> relevantObjectBindings) {
      this.relevantObjectBindings = relevantObjectBindings;
      return this;
    }

    public Builder addRelevantObjectBinding(FactObjectBindingDefinition relevantObjectBinding) {
      this.relevantObjectBindings = ListUtils.addToList(this.relevantObjectBindings, relevantObjectBinding);
      return this;
    }
  }

  @ApiModel(value = "FactObjectBindingDefinitionModel", description = "Defines to which Objects new Facts of a particular type can be linked.")
  public static class FactObjectBindingDefinition {
    @ApiModelProperty(value = "Type linked Objects must have", required = true)
    private final ObjectType.Info objectType;
    @ApiModelProperty(value = "Direction the link must have", required = true)
    private final Direction direction;

    public FactObjectBindingDefinition(ObjectType.Info objectType, Direction direction) {
      this.objectType = objectType;
      this.direction = direction;
    }

    public ObjectType.Info getObjectType() {
      return objectType;
    }

    public Direction getDirection() {
      return direction;
    }
  }

  @ApiModel(value = "FactTypeInfo", description = "Short summary of a FactType.")
  public class Info {
    @ApiModelProperty(value = "Uniquely identifies the FactType", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
    private final UUID id;
    @ApiModelProperty(value = "Name of the FactType", example = "ThreatActorAlias", required = true)
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
