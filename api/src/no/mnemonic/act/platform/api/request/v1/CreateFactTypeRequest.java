package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class CreateFactTypeRequest {

  @NotNull
  @Size(min = 1)
  private String name;
  private String validator;
  private String validatorParameter;
  private String entityHandler;
  private String entityHandlerParameter;
  @Valid
  @NotNull
  @Size(min = 1)
  private List<FactObjectBindingDefinition> relevantObjectBindings;

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

  public String getEntityHandler() {
    return entityHandler;
  }

  public CreateFactTypeRequest setEntityHandler(String entityHandler) {
    this.entityHandler = entityHandler;
    return this;
  }

  public String getEntityHandlerParameter() {
    return entityHandlerParameter;
  }

  public CreateFactTypeRequest setEntityHandlerParameter(String entityHandlerParameter) {
    this.entityHandlerParameter = entityHandlerParameter;
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

}
