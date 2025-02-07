package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatDeserializer;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Create a new Origin.")
public class CreateOriginRequest implements ValidatingRequest {

  @Schema(description = "Organization new Origin belongs to (takes Organization UUID)", example = "123e4567-e89b-12d3-a456-426655440000")
  private UUID organization;
  @Schema(description = "Name of new Origin. Needs to be unique per Namespace", example = "John Doe", requiredMode = REQUIRED)
  @NotBlank
  private String name;
  @Schema(description = "Longer description about new Origin", example = "John Doe from Doe Inc")
  private String description;
  @Schema(description = "How much new Origin is trusted (value between 0.0 and 1.0, default 0.8)", example = "0.8", requiredMode = REQUIRED)
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
