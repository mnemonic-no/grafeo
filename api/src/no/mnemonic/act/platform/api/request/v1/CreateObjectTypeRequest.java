package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateObjectTypeRequest {

  @NotNull
  @Size(min = 1)
  private String name;
  private String validator;
  private String validatorParameter;
  private String entityHandler;
  private String entityHandlerParameter;

  public String getName() {
    return name;
  }

  public CreateObjectTypeRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getValidator() {
    return validator;
  }

  public CreateObjectTypeRequest setValidator(String validator) {
    this.validator = validator;
    return this;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public CreateObjectTypeRequest setValidatorParameter(String validatorParameter) {
    this.validatorParameter = validatorParameter;
    return this;
  }

  public String getEntityHandler() {
    return entityHandler;
  }

  public CreateObjectTypeRequest setEntityHandler(String entityHandler) {
    this.entityHandler = entityHandler;
    return this;
  }

  public String getEntityHandlerParameter() {
    return entityHandlerParameter;
  }

  public CreateObjectTypeRequest setEntityHandlerParameter(String entityHandlerParameter) {
    this.entityHandlerParameter = entityHandlerParameter;
    return this;
  }

}
