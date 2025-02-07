package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;

import javax.validation.constraints.NotBlank;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Create a new ObjectType.")
public class CreateObjectTypeRequest implements ValidatingRequest {

  public enum IndexOption {
    Daily, TimeGlobal
  }

  @Schema(description = "Name of new ObjectType. Needs to be unique per Namespace", example = "ip", requiredMode = REQUIRED)
  @NotBlank
  private String name;
  @Schema(description = "Validator used to validate new Objects of this type", example = "RegexValidator", requiredMode = REQUIRED)
  @NotBlank
  private String validator;
  @Schema(description = "Parameters used to customize Validator", example = "(\\d+).(\\d+).(\\d+).(\\d+)")
  private String validatorParameter;
  @Schema(description = "Specify how Facts bound to Objects of this type will be indexed (default 'Daily')", example = "TimeGlobal")
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
