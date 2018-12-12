package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Create a new meta Fact.")
public class CreateMetaFactRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @ApiModelProperty(value = "Type of new meta Fact. Can either be the UUID or name of an existing FactType",
          example = "Observation", required = true)
  @NotBlank
  private String type;
  @ApiModelProperty(value = "Value of new meta Fact (can be empty if allowed by FactType)", example = "2016-09-28T21:26:22Z")
  private String value;
  @ApiModelProperty(value = "Set owner of new meta Fact. If not set the current user's organization will be used (takes Organization UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID organization;
  @ApiModelProperty(value = "Set Source of new meta Fact. If not set the current user will be used as Source (takes Source UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID source;
  @ApiModelProperty(value = "Set access mode of new meta Fact. If not set the access mode from the referenced Fact will be used")
  private AccessMode accessMode;
  @ApiModelProperty(value = "If set adds a comment to new meta Fact", example = "Hello World!")
  private String comment;
  @ApiModelProperty(value = "If set defines explicitly who has access to new meta Fact (takes Subject UUIDs)")
  private List<UUID> acl;
  // TODO: Add confidenceLevel once defined.

  public UUID getFact() {
    return fact;
  }

  public CreateMetaFactRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public String getType() {
    return type;
  }

  public CreateMetaFactRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getValue() {
    return value;
  }

  public CreateMetaFactRequest setValue(String value) {
    this.value = value;
    return this;
  }

  public UUID getOrganization() {
    return organization;
  }

  public CreateMetaFactRequest setOrganization(UUID organization) {
    this.organization = organization;
    return this;
  }

  public UUID getSource() {
    return source;
  }

  public CreateMetaFactRequest setSource(UUID source) {
    this.source = source;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public CreateMetaFactRequest setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public CreateMetaFactRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public List<UUID> getAcl() {
    return acl;
  }

  public CreateMetaFactRequest setAcl(List<UUID> acl) {
    this.acl = acl;
    return this;
  }

  public CreateMetaFactRequest addAcl(UUID acl) {
    this.acl = ListUtils.addToList(this.acl, acl);
    return this;
  }

}
