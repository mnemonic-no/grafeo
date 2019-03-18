package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Create a new Fact.")
public class CreateFactRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Type of new Fact. Can either be the UUID or name of an existing FactType",
          example = "ThreatActorAlias", required = true)
  @NotBlank
  private String type;
  @ApiModelProperty(value = "Value of new Fact (can be empty if allowed by FactType)", example = "APT28")
  private String value;
  @ApiModelProperty(value = "Set owner of new Fact. If not set the current user's organization will be used (takes Organization UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID organization;
  @ApiModelProperty(value = "Set Source of new Fact. If not set the current user will be used as Source (takes Source UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID source;
  @ApiModelProperty(value = "Set access mode of new Fact (default 'RoleBased')")
  private AccessMode accessMode = AccessMode.RoleBased;
  @ApiModelProperty(value = "If set adds a comment to new Fact", example = "Hello World!")
  private String comment;
  @ApiModelProperty(value = "If set defines explicitly who has access to new Fact (takes Subject UUIDs)")
  private List<UUID> acl;
  @ApiModelProperty(value = "Set Object which is linked to new Fact as source (takes Object UUID or Object identified by 'type/value')",
          example = "ThreatActor/Sofacy")
  private String sourceObject;
  @ApiModelProperty(value = "Set Object which is linked to new Fact as destination (takes Object UUID or Object identified by 'type/value')",
          example = "ThreatActor/FancyBear")
  private String destinationObject;
  @ApiModelProperty(value = "If true the binding between source Object, Fact and destination Object is bidirectional (default 'false')")
  private boolean bidirectionalBinding;
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
    this.acl = ObjectUtils.ifNotNull(acl, ListUtils::list);
    return this;
  }

  public CreateFactRequest addAcl(UUID acl) {
    this.acl = ListUtils.addToList(this.acl, acl);
    return this;
  }

  public String getSourceObject() {
    return sourceObject;
  }

  public CreateFactRequest setSourceObject(String sourceObject) {
    this.sourceObject = sourceObject;
    return this;
  }

  public String getDestinationObject() {
    return destinationObject;
  }

  public CreateFactRequest setDestinationObject(String destinationObject) {
    this.destinationObject = destinationObject;
    return this;
  }

  public boolean isBidirectionalBinding() {
    return bidirectionalBinding;
  }

  public CreateFactRequest setBidirectionalBinding(boolean bidirectionalBinding) {
    this.bidirectionalBinding = bidirectionalBinding;
    return this;
  }

}
