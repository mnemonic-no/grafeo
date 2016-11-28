package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

public class UpdateObjectTypeRequest {

  @NotNull
  private UUID id;
  @Size(min = 1)
  private String name;

  public UUID getId() {
    return id;
  }

  public UpdateObjectTypeRequest setId(UUID id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public UpdateObjectTypeRequest setName(String name) {
    this.name = name;
    return this;
  }

}
