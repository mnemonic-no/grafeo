package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public class UpdateFactTypeRequest {

  @NotNull
  private UUID id;
  @Size(min = 1)
  private String name;
  @Valid
  private List<FactObjectBindingDefinition> addObjectBindings;

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

  public List<FactObjectBindingDefinition> getAddObjectBindings() {
    return addObjectBindings;
  }

  public UpdateFactTypeRequest setAddObjectBindings(List<FactObjectBindingDefinition> addObjectBindings) {
    this.addObjectBindings = addObjectBindings;
    return this;
  }

  public UpdateFactTypeRequest addAddObjectBinding(FactObjectBindingDefinition addObjectBinding) {
    this.addObjectBindings = ListUtils.addToList(this.addObjectBindings, addObjectBinding);
    return this;
  }

}
