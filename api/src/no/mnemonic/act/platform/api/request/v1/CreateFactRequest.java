package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Create a new Fact.")
public class CreateFactRequest {

  @ApiModelProperty(value = "Type of new Fact. Can either be the UUID or name of an existing FactType",
          example = "ThreatActorAlias", required = true)
  @NotNull
  @Size(min = 1)
  private String type;
  @ApiModelProperty(value = "Value of new Fact", example = "APT1", required = true)
  @NotNull
  @Size(min = 1)
  private String value;
  @ApiModelProperty(value = "Set if new Fact should reference an existing Fact (takes Fact UUID)")
  private UUID inReferenceTo;
  @ApiModelProperty(value = "Set owner of new Fact. If not set the current user's organization will be used (takes Organization UUID)")
  private UUID organization;
  @ApiModelProperty(value = "Set Source of new Fact. If not set the current user will be used as Source (takes Source UUID)")
  private UUID source;
  @ApiModelProperty(value = "Set access mode of new Fact (default 'RoleBased')")
  private AccessMode accessMode = AccessMode.RoleBased;
  @ApiModelProperty(value = "If set adds a comment to new Fact", example = "Hello World!")
  private String comment;
  @ApiModelProperty(value = "If set defines explicitly who has access to new Fact (takes Subject UUIDs)")
  private List<UUID> acl;
  @ApiModelProperty(value = "Define to which Objects the new Fact links", required = true)
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

  @ApiModel(value = "FactObjectBindingRequest", description = "Define to which Objects a new Fact links")
  public static class FactObjectBinding {
    // Either objectID or objectType + objectValue must be set.
    @ApiModelProperty(value = "UUID of an existing Object. Set either 'objectID' or 'objectType' plus 'objectValue'")
    private UUID objectID;
    @ApiModelProperty(value = "Type of an Object (takes type name)", example = "ip")
    private String objectType;
    @ApiModelProperty(value = "Value of an Object", example = "27.13.4.125")
    private String objectValue;
    @ApiModelProperty(value = "Direction of link between Fact and Object", required = true)
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
