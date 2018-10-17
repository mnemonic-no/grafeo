package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@ApiModel(value = "MetaFactBindingDefinitionRequest", description = "Define to which Facts new meta Facts of a particular type can be linked.")
public class MetaFactBindingDefinition {

  @ApiModelProperty(value = "Specify the type any Fact must have when referenced by a new meta Fact (takes FactType UUID)", required = true)
  @NotNull
  private UUID factType;

  public UUID getFactType() {
    return factType;
  }

  public MetaFactBindingDefinition setFactType(UUID factType) {
    this.factType = factType;
    return this;
  }

}
