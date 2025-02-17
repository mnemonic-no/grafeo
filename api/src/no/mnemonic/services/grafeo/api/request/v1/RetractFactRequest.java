package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatDeserializer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;

@Schema(description = "Retract an existing Fact.")
public class RetractFactRequest implements ValidatingRequest {

  @Schema(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @Schema(description = "Set owner of new Fact. If not set the Origin's organization will be used (takes Organization UUID or name)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private String organization;
  @Schema(description = "Set Origin of new Fact. If not set the current user will be used as Origin (takes Origin UUID or name)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private String origin;
  @Schema(description = "Set confidence of new Fact. If not set the FactType's default confidence will be used " +
          "(value between 0.0 and 1.0)", example = "0.9")
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private Float confidence;
  @Schema(description = "Set access mode of new Fact. If not set the access mode from the retracted Fact will be used")
  private AccessMode accessMode;
  @Schema(description = "If set adds a comment to new Fact", example = "Hello World!")
  private String comment;
  @Schema(description = "If set defines explicitly who has access to new Fact (takes Subject UUIDs or names)")
  private List<String> acl;

  public UUID getFact() {
    return fact;
  }

  public RetractFactRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public RetractFactRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrigin() {
    return origin;
  }

  public RetractFactRequest setOrigin(String origin) {
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

  public List<String> getAcl() {
    return acl;
  }

  public RetractFactRequest setAcl(List<String> acl) {
    this.acl = ObjectUtils.ifNotNull(acl, ListUtils::list);
    return this;
  }

  public RetractFactRequest addAcl(String acl) {
    this.acl = ListUtils.addToList(this.acl, acl);
    return this;
  }

}
