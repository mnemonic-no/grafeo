package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatDeserializer;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
  @ApiModelProperty(value = "Set owner of new meta Fact. If not set the Origin's organization will be used (takes Organization UUID or name)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private String organization;
  @ApiModelProperty(value = "Set Origin of new meta Fact. If not set the current user will be used as Origin (takes Origin UUID or name)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private String origin;
  @ApiModelProperty(value = "Set confidence of new meta Fact. If not set the FactType's default confidence will be used " +
          "(value between 0.0 and 1.0)", example = "0.9")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float confidence;
  @ApiModelProperty(value = "Set access mode of new meta Fact. If not set the access mode from the referenced Fact will be used")
  private AccessMode accessMode;
  @ApiModelProperty(value = "If set adds a comment to new meta Fact", example = "Hello World!")
  private String comment;
  @ApiModelProperty(value = "If set defines explicitly who has access to new meta Fact (takes Subject UUIDs or names)")
  private List<String> acl;

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

  public String getOrganization() {
    return organization;
  }

  public CreateMetaFactRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrigin() {
    return origin;
  }

  public CreateMetaFactRequest setOrigin(String origin) {
    this.origin = origin;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public Float getConfidence() {
    return confidence;
  }

  public CreateMetaFactRequest setConfidence(Float confidence) {
    this.confidence = confidence;
    return this;
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

  public List<String> getAcl() {
    return acl;
  }

  public CreateMetaFactRequest setAcl(List<String> acl) {
    this.acl = ObjectUtils.ifNotNull(acl, ListUtils::list);
    return this;
  }

  public CreateMetaFactRequest addAcl(String acl) {
    this.acl = ListUtils.addToList(this.acl, acl);
    return this;
  }

}
