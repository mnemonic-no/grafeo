package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@ApiModel(description = "Create a new FactType.")
public class CreateFactTypeRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Name of new FactType. Needs to be unique per Namespace", example = "ThreatActorAlias", required = true)
  @NotBlank
  private String name;
  @ApiModelProperty(value = "Validator used to validate new Facts of this type", example = "RegexValidator", required = true)
  @NotBlank
  private String validator;
  @ApiModelProperty(value = "Parameters used to customize Validator", example = "[^ ]+")
  private String validatorParameter;
  @ApiModelProperty(value = "Define to which Objects new Facts of this type can be linked")
  private List<@Valid FactObjectBindingDefinition> relevantObjectBindings;
  @ApiModelProperty(value = "Define to which Facts new meta Facts of this type can be linked")
  private List<@Valid MetaFactBindingDefinition> relevantFactBindings;

  public String getName() {
    return name;
  }

  public CreateFactTypeRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getValidator() {
    return validator;
  }

  public CreateFactTypeRequest setValidator(String validator) {
    this.validator = validator;
    return this;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public CreateFactTypeRequest setValidatorParameter(String validatorParameter) {
    this.validatorParameter = validatorParameter;
    return this;
  }

  public List<FactObjectBindingDefinition> getRelevantObjectBindings() {
    return relevantObjectBindings;
  }

  public CreateFactTypeRequest setRelevantObjectBindings(List<FactObjectBindingDefinition> relevantObjectBindings) {
    this.relevantObjectBindings = relevantObjectBindings;
    return this;
  }

  public CreateFactTypeRequest addRelevantObjectBinding(FactObjectBindingDefinition relevantObjectBinding) {
    this.relevantObjectBindings = ListUtils.addToList(this.relevantObjectBindings, relevantObjectBinding);
    return this;
  }

  public List<MetaFactBindingDefinition> getRelevantFactBindings() {
    return relevantFactBindings;
  }

  public CreateFactTypeRequest setRelevantFactBindings(List<MetaFactBindingDefinition> relevantFactBindings) {
    this.relevantFactBindings = relevantFactBindings;
    return this;
  }

  public CreateFactTypeRequest addRelevantFactBinding(MetaFactBindingDefinition relevantFactBinding) {
    this.relevantFactBindings = ListUtils.addToList(this.relevantFactBindings, relevantFactBinding);
    return this;
  }

}
