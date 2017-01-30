package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@ApiModel(description = "Create a new ObjectType.")
public class CreateObjectTypeRequest {

  @ApiModelProperty(value = "Name of new ObjectType. Needs to be unique per Namespace", example = "ip", required = true)
  @NotNull
  @Size(min = 1)
  private String name;
  @ApiModelProperty(value = "Validator used to validate new Objects of this type", example = "RegexValidator")
  private String validator;
  @ApiModelProperty(value = "Parameters used to customize Validator", example = "(\\d+).(\\d+).(\\d+).(\\d+)")
  private String validatorParameter;
  @ApiModelProperty(value = "EntityHandler used to store new Objects of this type", example = "IpEntityHandler")
  private String entityHandler;
  @ApiModelProperty(value = "Parameters used to customize EntityHandler")
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
