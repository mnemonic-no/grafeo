package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.RoundingFloatDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

@ApiModel(description = "Create a new Fact.")
public class CreateFactRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Type of new Fact. Can either be the UUID or name of an existing FactType",
          example = "ThreatActorAlias", required = true)
  @NotBlank
  private String type;
  @ApiModelProperty(value = "Value of new Fact (can be empty if allowed by FactType)", example = "APT28")
  private String value;
  @ApiModelProperty(value = "Set owner of new Fact. If not set the Origins's organization will be used (takes Organization UUID or name)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private String organization;
  @ApiModelProperty(value = "Set Origin of new Fact. If not set the current user will be used as Origin (takes Origin UUID or name)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private String origin;
  @ApiModelProperty(value = "Set confidence of new Fact. If not set the FactType's default confidence will be used " +
          "(value between 0.0 and 1.0)", example = "0.9")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float confidence;
  @ApiModelProperty(value = "Set access mode of new Fact (default 'RoleBased')")
  private AccessMode accessMode = AccessMode.RoleBased;
  @ApiModelProperty(value = "If set adds a comment to new Fact", example = "Hello World!")
  private String comment;
  @ApiModelProperty(value = "If set defines explicitly who has access to new Fact (takes Subject UUIDs or names)")
  private List<String> acl;
  @ApiModelProperty(value = "Set Object which is linked to new Fact as source (takes Object UUID or Object identified by 'type/value')",
          example = "ThreatActor/Sofacy")
  private String sourceObject;
  @ApiModelProperty(value = "Set Object which is linked to new Fact as destination (takes Object UUID or Object identified by 'type/value')",
          example = "ThreatActor/FancyBear")
  private String destinationObject;
  @ApiModelProperty(value = "If true the binding between source Object, Fact and destination Object is bidirectional (default 'false')")
  private boolean bidirectionalBinding;

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

  public String getOrganization() {
    return organization;
  }

  public CreateFactRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrigin() {
    return origin;
  }

  public CreateFactRequest setOrigin(String origin) {
    this.origin = origin;
    return this;
  }

  public Float getConfidence() {
    return confidence;
  }

  public CreateFactRequest setConfidence(Float confidence) {
    this.confidence = confidence;
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

  public List<String> getAcl() {
    return acl;
  }

  public CreateFactRequest setAcl(List<String> acl) {
    this.acl = ObjectUtils.ifNotNull(acl, ListUtils::list);
    return this;
  }

  public CreateFactRequest addAcl(String acl) {
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
