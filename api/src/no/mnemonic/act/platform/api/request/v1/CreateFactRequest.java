package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public class CreateFactRequest {

  @NotNull
  @Size(min = 1)
  private String type;
  @NotNull
  @Size(min = 1)
  private String value;
  private UUID inReferenceTo;
  private UUID organization;
  private UUID source;
  private AccessMode accessMode = AccessMode.RoleBased;
  private String comment;
  private List<UUID> acl;
  @Valid
  @NotNull
  @Size(min = 1)
  private List<FactObjectBinding> bindings;
  // TODO: Add confidenceLevel once defined.

  public String getType() {
    return type;
  }

  public CreateFactRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getValue() {
    return value;
  }

  public CreateFactRequest setValue(String value) {
    this.value = value;
    return this;
  }

  public UUID getInReferenceTo() {
    return inReferenceTo;
  }

  public CreateFactRequest setInReferenceTo(UUID inReferenceTo) {
    this.inReferenceTo = inReferenceTo;
    return this;
  }

  public UUID getOrganization() {
    return organization;
  }

  public CreateFactRequest setOrganization(UUID organization) {
    this.organization = organization;
    return this;
  }

  public UUID getSource() {
    return source;
  }

  public CreateFactRequest setSource(UUID source) {
    this.source = source;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public CreateFactRequest setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public CreateFactRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public List<UUID> getAcl() {
    return acl;
  }

  public CreateFactRequest setAcl(List<UUID> acl) {
    this.acl = acl;
    return this;
  }

  public CreateFactRequest addAcl(UUID acl) {
    this.acl = ListUtils.addToList(this.acl, acl);
    return this;
  }

  public List<FactObjectBinding> getBindings() {
    return bindings;
  }

  public CreateFactRequest setBindings(List<FactObjectBinding> bindings) {
    this.bindings = bindings;
    return this;
  }

  public CreateFactRequest addBinding(FactObjectBinding binding) {
    this.bindings = ListUtils.addToList(this.bindings, binding);
    return this;
  }

  public static class FactObjectBinding {
    // Either objectID or objectType + objectValue must be set.
    private UUID objectID;
    private String objectType;
    private String objectValue;
    @NotNull
    private Direction direction;

    public UUID getObjectID() {
      return objectID;
    }

    public FactObjectBinding setObjectID(UUID objectID) {
      this.objectID = objectID;
      return this;
    }

    public String getObjectType() {
      return objectType;
    }

    public FactObjectBinding setObjectType(String objectType) {
      this.objectType = objectType;
      return this;
    }

    public String getObjectValue() {
      return objectValue;
    }

    public FactObjectBinding setObjectValue(String objectValue) {
      this.objectValue = objectValue;
      return this;
    }

    public Direction getDirection() {
      return direction;
    }

    public FactObjectBinding setDirection(Direction direction) {
      this.direction = direction;
      return this;
    }
  }

}
