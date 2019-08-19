package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Update an existing FactType.")
public class UpdateFactTypeRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID id;
  @ApiModelProperty(value = "If set updates the name of the FactType", example = "ThreatActorAlias")
  @Size(min = 1)
  private String name;
  @ApiModelProperty(value = "If set updates the default confidence of the FactType (value between 0.0 and 1.0)", example = "0.9")
  @Min(0)
  @Max(1)
  private Float defaultConfidence;
  @ApiModelProperty(value = "If set adds additional possible links between Facts and Objects")
  private List<@Valid FactObjectBindingDefinition> addObjectBindings;
  @ApiModelProperty(value = "If set adds additional possible links for meta Facts")
  private List<@Valid MetaFactBindingDefinition> addFactBindings;

  public UUID getId() {
    return id;
  }

  public UpdateFactTypeRequest setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public UpdateFactTypeRequest setName(String name) {
    this.name = name;
    return this;
  }

  public Float getDefaultConfidence() {
    return defaultConfidence;
  }

  public UpdateFactTypeRequest setDefaultConfidence(Float defaultConfidence) {
    this.defaultConfidence = defaultConfidence;
    return this;
  }

  public List<FactObjectBindingDefinition> getAddObjectBindings() {
    return addObjectBindings;
  }

  public UpdateFactTypeRequest setAddObjectBindings(List<FactObjectBindingDefinition> addObjectBindings) {
    this.addObjectBindings = ObjectUtils.ifNotNull(addObjectBindings, ListUtils::list);
    return this;
  }

  public UpdateFactTypeRequest addAddObjectBinding(FactObjectBindingDefinition addObjectBinding) {
    this.addObjectBindings = ListUtils.addToList(this.addObjectBindings, addObjectBinding);
    return this;
  }

  public List<MetaFactBindingDefinition> getAddFactBindings() {
    return addFactBindings;
  }

  public UpdateFactTypeRequest setAddFactBindings(List<MetaFactBindingDefinition> addFactBindings) {
    this.addFactBindings = ObjectUtils.ifNotNull(addFactBindings, ListUtils::list);
    return this;
  }

  public UpdateFactTypeRequest addAddFactBinding(MetaFactBindingDefinition addFactBinding) {
    this.addFactBindings = ListUtils.addToList(this.addFactBindings, addFactBinding);
    return this;
  }

}
