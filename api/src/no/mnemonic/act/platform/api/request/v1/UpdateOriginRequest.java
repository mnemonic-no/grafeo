package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.UUID;

@ApiModel(description = "Update an existing Origin.")
public class UpdateOriginRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID id;
  @ApiModelProperty(value = "If set updates the Organization of the Origin (takes Organization UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID organization;
  @ApiModelProperty(value = "If set updates the name of the Origin. Needs to be unique per Namespace", example = "John Doe")
  private String name;
  @ApiModelProperty(value = "If set updates the description of the Origin.", example = "John Doe from Doe Inc")
  private String description;
  @ApiModelProperty(value = "If set updates the trust value of the Origin (value between 0.0 and 1.0)", example = "0.8")
  @Min(0)
  @Max(1)
  private Float trust;

  public UUID getId() {
    return id;
  }

  public UpdateOriginRequest setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getOrganization() {
    return organization;
  }

  public UpdateOriginRequest setOrganization(UUID organization) {
    this.organization = organization;
    return this;
  }

  public String getName() {
    return name;
  }

  public UpdateOriginRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public UpdateOriginRequest setDescription(String description) {
    this.description = description;
    return this;
  }

  public Float getTrust() {
    return trust;
  }

  public UpdateOriginRequest setTrust(Float trust) {
    this.trust = trust;
    return this;
  }

}
