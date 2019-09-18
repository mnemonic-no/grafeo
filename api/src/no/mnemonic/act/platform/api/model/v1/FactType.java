package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.RoundingFloatSerializer;
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
  @ApiModelProperty(value = "Confidence assigned by default to new Facts of this type", example = "0.9", required = true)
  @JsonSerialize(using = RoundingFloatSerializer.class)
  private final float defaultConfidence;
  @ApiModelProperty(value = "Validator used to validate new Facts of this type", example = "RegexValidator", required = true)
  private final String validator;
  @ApiModelProperty(value = "Parameters used to customize Validator", example = "(\\d+).(\\d+).(\\d+).(\\d+)")
  private final String validatorParameter;
  @ApiModelProperty(value = "Defines to which Objects new Facts of this type can be linked")
  private final List<FactObjectBindingDefinition> relevantObjectBindings;
  @ApiModelProperty(value = "Defines to which Facts new meta Facts of this type can be linked")
  private final List<MetaFactBindingDefinition> relevantFactBindings;

  private FactType(UUID id,
                   Namespace namespace,
                   String name,
                   float defaultConfidence,
                   String validator,
                   String validatorParameter,
                   List<FactObjectBindingDefinition> relevantObjectBindings,
                   List<MetaFactBindingDefinition> relevantFactBindings) {
    this.id = id;
    this.namespace = namespace;
    this.name = name;
    this.defaultConfidence = defaultConfidence;
    this.validator = validator;
    this.validatorParameter = validatorParameter;
    this.relevantObjectBindings = ObjectUtils.ifNotNull(relevantObjectBindings, Collections::unmodifiableList);
    this.relevantFactBindings = ObjectUtils.ifNotNull(relevantFactBindings, Collections::unmodifiableList);
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

  public float getDefaultConfidence() {
    return defaultConfidence;
  }

  public String getValidator() {
    return validator;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public List<FactObjectBindingDefinition> getRelevantObjectBindings() {
    return relevantObjectBindings;
  }

  public List<MetaFactBindingDefinition> getRelevantFactBindings() {
    return relevantFactBindings;
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
    private float defaultConfidence;
    private String validator;
    private String validatorParameter;
    private List<FactObjectBindingDefinition> relevantObjectBindings;
    private List<MetaFactBindingDefinition> relevantFactBindings;

    private Builder() {
    }

    public FactType build() {
      return new FactType(id, namespace, name, defaultConfidence, validator, validatorParameter, relevantObjectBindings, relevantFactBindings);
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

    public Builder setDefaultConfidence(float defaultConfidence) {
      this.defaultConfidence = defaultConfidence;
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

    public Builder setRelevantObjectBindings(List<FactObjectBindingDefinition> relevantObjectBindings) {
      this.relevantObjectBindings = ObjectUtils.ifNotNull(relevantObjectBindings, ListUtils::list);
      return this;
    }

    public Builder addRelevantObjectBinding(FactObjectBindingDefinition relevantObjectBinding) {
      this.relevantObjectBindings = ListUtils.addToList(this.relevantObjectBindings, relevantObjectBinding);
      return this;
    }

    public Builder setRelevantFactBindings(List<MetaFactBindingDefinition> relevantFactBindings) {
      this.relevantFactBindings = ObjectUtils.ifNotNull(relevantFactBindings, ListUtils::list);
      return this;
    }

    public Builder addRelevantFactBinding(MetaFactBindingDefinition relevantFactBinding) {
      this.relevantFactBindings = ListUtils.addToList(this.relevantFactBindings, relevantFactBinding);
      return this;
    }
  }

  @ApiModel(value = "FactObjectBindingDefinitionModel", description = "Defines to which Objects new Facts of a particular type can be linked.")
  public static class FactObjectBindingDefinition {
    @ApiModelProperty(value = "Specifies the type any Object must have when linked as source to a Fact (can be NULL)")
    private final ObjectType.Info sourceObjectType;
    @ApiModelProperty(value = "Specifies the type any Object must have when linked as destination to a Fact (can be NULL)")
    private final ObjectType.Info destinationObjectType;
    @ApiModelProperty(value = "Specifies if the binding between source Object, Fact and destination Object must be bidirectional", required = true)
    private final boolean bidirectionalBinding;

    public FactObjectBindingDefinition(ObjectType.Info sourceObjectType, ObjectType.Info destinationObjectType, boolean bidirectionalBinding) {
      this.sourceObjectType = sourceObjectType;
      this.destinationObjectType = destinationObjectType;
      this.bidirectionalBinding = bidirectionalBinding;
    }

    public ObjectType.Info getSourceObjectType() {
      return sourceObjectType;
    }

    public ObjectType.Info getDestinationObjectType() {
      return destinationObjectType;
    }

    public boolean isBidirectionalBinding() {
      return bidirectionalBinding;
    }
  }

  @ApiModel(value = "MetaFactBindingDefinitionModel", description = "Defines to which Facts new meta Facts of a particular type can be linked.")
  public static class MetaFactBindingDefinition {
    @ApiModelProperty(value = "Specifies the type any Fact must have when referenced by a new meta Fact", required = true)
    private final FactType.Info factType;

    public MetaFactBindingDefinition(FactType.Info factType) {
      this.factType = factType;
    }

    public FactType.Info getFactType() {
      return factType;
    }
  }

  @ApiModel(value = "FactTypeInfo", description = "Short summary of a FactType.")
  public static class Info {
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
