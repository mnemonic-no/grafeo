package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatDeserializer;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Update an existing FactType.")
public class UpdateFactTypeRequest implements ValidatingRequest {

  @Schema(hidden = true)
  @ServiceNotNull
  private UUID id;
  @Schema(description = "If set updates the name of the FactType", example = "ThreatActorAlias")
  @Size(min = 1)
  private String name;
  @Schema(description = "If set updates the default confidence of the FactType (value between 0.0 and 1.0)", example = "0.9")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float defaultConfidence;
  @Schema(description = "If set adds additional possible links between Facts and Objects")
  private List<@Valid FactObjectBindingDefinition> addObjectBindings;
  @Schema(description = "If set adds additional possible links for meta Facts")
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
