package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.List;
import java.util.UUID;

@ApiModel(description = "Retract an existing Fact.")
public class RetractFactRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @ApiModelProperty(value = "Set owner of new Fact. If not set the current user's organization will be used (takes Organization UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID organization;
  @ApiModelProperty(value = "Set Source of new Fact. If not set the current user will be used as Source (takes Source UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID source;
  @ApiModelProperty(value = "Set access mode of new Fact. If not set the access mode from the retracted Fact will be used")
  private AccessMode accessMode;
  @ApiModelProperty(value = "If set adds a comment to new Fact", example = "Hello World!")
  private String comment;
  @ApiModelProperty(value = "If set defines explicitly who has access to new Fact (takes Subject UUIDs)")
  private List<UUID> acl;
  // TODO: Add confidenceLevel once defined.

  public UUID getFact() {
    return fact;
  }

  public RetractFactRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public UUID getOrganization() {
    return organization;
  }

  public RetractFactRequest setOrganization(UUID organization) {
    this.organization = organization;
    return this;
  }

  public UUID getSource() {
    return source;
  }

  public RetractFactRequest setSource(UUID source) {
    this.source = source;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public RetractFactRequest setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public RetractFactRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public List<UUID> getAcl() {
    return acl;
  }

  public RetractFactRequest setAcl(List<UUID> acl) {
    this.acl = ObjectUtils.ifNotNull(acl, ListUtils::list);
    return this;
  }

  public RetractFactRequest addAcl(UUID acl) {
    this.acl = ListUtils.addToList(this.acl, acl);
    return this;
  }

}
