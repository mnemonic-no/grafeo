package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Retract an existing Fact.")
public class RetractFactRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @ApiModelProperty(value = "Set owner of new Fact. If not set the Origin's organization will be used (takes Organization UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID organization;
  @ApiModelProperty(value = "Set Origin of new Fact. If not set the current user will be used as Origin (takes Origin UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID origin;
  @ApiModelProperty(value = "Set confidence of new Fact. If not set the FactType's default confidence will be used " +
          "(value between 0.0 and 1.0)", example = "0.9")
  @Min(0)
  @Max(1)
  private Float confidence;
  @ApiModelProperty(value = "Set access mode of new Fact. If not set the access mode from the retracted Fact will be used")
  private AccessMode accessMode;
  @ApiModelProperty(value = "If set adds a comment to new Fact", example = "Hello World!")
  private String comment;
  @ApiModelProperty(value = "If set defines explicitly who has access to new Fact (takes Subject UUIDs)")
  private List<UUID> acl;

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

  public UUID getOrigin() {
    return origin;
  }

  public RetractFactRequest setOrigin(UUID origin) {
    this.origin = origin;
    return this;
  }

  public Float getConfidence() {
    return confidence;
  }

  public RetractFactRequest setConfidence(Float confidence) {
    this.confidence = confidence;
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
