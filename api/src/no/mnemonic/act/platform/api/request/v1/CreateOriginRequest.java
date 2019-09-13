package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.RoundingFloatDeserializer;
import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.UUID;

@ApiModel(description = "Create a new Origin.")
public class CreateOriginRequest implements ValidatingRequest {

  @ApiModelProperty(value = "Organization new Origin belongs to (takes Organization UUID)", example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID organization;
  @ApiModelProperty(value = "Name of new Origin. Needs to be unique per Namespace", example = "John Doe", required = true)
  @NotBlank
  private String name;
  @ApiModelProperty(value = "Longer description about new Origin", example = "John Doe from Doe Inc")
  private String description;
  @ApiModelProperty(value = "How much new Origin is trusted (value between 0.0 and 1.0, default 0.8)", example = "0.8", required = true)
  @JsonDeserialize(using = RoundingFloatDeserializer.class)
  @Min(0)
  @Max(1)
  private float trust = 0.8f;

  public UUID getOrganization() {
    return organization;
  }

  public CreateOriginRequest setOrganization(UUID organization) {
    this.organization = organization;
    return this;
  }

  public String getName() {
    return name;
  }

  public CreateOriginRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public CreateOriginRequest setDescription(String description) {
    this.description = description;
    return this;
  }

  public float getTrust() {
    return trust;
  }

  public CreateOriginRequest setTrust(float trust) {
    this.trust = trust;
    return this;
  }

}
