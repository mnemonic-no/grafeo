package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.NotBlank;

@ApiModel(description = "Create a new ObjectType.")
public class CreateObjectTypeRequest implements ValidatingRequest {

  public enum IndexOption {
    Daily, TimeGlobal
  }

  @ApiModelProperty(value = "Name of new ObjectType. Needs to be unique per Namespace", example = "ip", required = true)
  @NotBlank
  private String name;
  @ApiModelProperty(value = "Validator used to validate new Objects of this type", example = "RegexValidator", required = true)
  @NotBlank
  private String validator;
  @ApiModelProperty(value = "Parameters used to customize Validator", example = "(\\d+).(\\d+).(\\d+).(\\d+)")
  private String validatorParameter;
  @ApiModelProperty(value = "Specify how Facts bound to Objects of this type will be indexed (default 'Daily')", example = "TimeGlobal")
  private IndexOption indexOption = IndexOption.Daily;

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

  public IndexOption getIndexOption() {
    return indexOption;
  }

  public CreateObjectTypeRequest setIndexOption(IndexOption indexOption) {
    this.indexOption = indexOption;
    return this;
  }

}
