package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatDeserializer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Create a new FactType.")
public class CreateFactTypeRequest implements ValidatingRequest {

  @Schema(description = "Name of new FactType. Needs to be unique per Namespace", example = "ThreatActorAlias", requiredMode = REQUIRED)
  @NotBlank
  private String name;
  @Schema(description = "Confidence assigned by default to new Facts of this type (value between 0.0 and 1.0, default 1.0)",
          example = "0.9", requiredMode = REQUIRED)
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private float defaultConfidence = 1.0f;
  @Schema(description = "Validator used to validate new Facts of this type", example = "RegexValidator", requiredMode = REQUIRED)
  @NotBlank
  private String validator;
  @Schema(description = "Parameters used to customize Validator", example = "[^ ]+")
  private String validatorParameter;
  @Schema(description = "Define to which Objects new Facts of this type can be linked")
  private List<@Valid FactObjectBindingDefinition> relevantObjectBindings;
  @Schema(description = "Define to which Facts new meta Facts of this type can be linked")
  private List<@Valid MetaFactBindingDefinition> relevantFactBindings;

  public String getName() {
    return name;
  }

  public CreateFactTypeRequest setName(String name) {
    this.name = name;
    return this;
  }

  public float getDefaultConfidence() {
    return defaultConfidence;
  }

  public CreateFactTypeRequest setDefaultConfidence(float defaultConfidence) {
    this.defaultConfidence = defaultConfidence;
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
    this.relevantObjectBindings = ObjectUtils.ifNotNull(relevantObjectBindings, ListUtils::list);
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
    this.relevantFactBindings = ObjectUtils.ifNotNull(relevantFactBindings, ListUtils::list);
    return this;
  }

  public CreateFactTypeRequest addRelevantFactBinding(MetaFactBindingDefinition relevantFactBinding) {
    this.relevantFactBindings = ListUtils.addToList(this.relevantFactBindings, relevantFactBinding);
    return this;
  }

}
