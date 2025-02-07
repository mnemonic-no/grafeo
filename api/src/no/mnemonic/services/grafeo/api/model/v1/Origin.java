package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatSerializer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Every piece of information (i.e. Facts) put into the system is marked with an Origin " +
        "in order to identify where the information came from."
)
public class Origin {

  public enum Type {
    Group, User
  }

  public enum Flag {
    Deleted
  }

  @Schema(description = "Uniquely identifies the Origin", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Namespace the Origin belongs to", requiredMode = REQUIRED)
  private final Namespace namespace;
  @Schema(description = "Organization the Origin belongs to")
  private final Organization.Info organization;
  @Schema(description = "Name of the Origin", example = "John Doe", requiredMode = REQUIRED)
  private final String name;
  @Schema(description = "Longer description about the Origin", example = "John Doe from Doe Inc")
  private final String description;
  @Schema(description = "How much the Origin is trusted", example = "0.8", requiredMode = REQUIRED)
  @JsonSerialize(using = RoundingFloatSerializer.class)
  private final float trust;
  @Schema(description = "Type of the Origin (group or individual user)", requiredMode = REQUIRED)
  private final Type type;
  @Schema(description = "Contains any flags set on the Origin")
  private final Set<Flag> flags;

  private Origin(UUID id, Namespace namespace, Organization.Info organization, String name, String description,
                 float trust, Type type, Set<Flag> flags) {
    this.id = id;
    this.namespace = namespace;
    this.organization = organization;
    this.name = name;
    this.description = description;
    this.trust = trust;
    this.type = type;
    this.flags = ObjectUtils.ifNotNull(flags, Collections::unmodifiableSet);
  }

  public UUID getId() {
    return id;
  }

  public Namespace getNamespace() {
    return namespace;
  }

  public Organization.Info getOrganization() {
    return organization;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public float getTrust() {
    return trust;
  }

  public Type getType() {
    return type;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public Info toInfo() {
    return new Info(id, name);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private Namespace namespace;
    private Organization.Info organization;
    private String name;
    private String description;
    private float trust;
    private Type type;
    private Set<Flag> flags;

    private Builder() {
    }

    public Origin build() {
      return new Origin(id, namespace, organization, name, description, trust, type, flags);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setNamespace(Namespace namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder setOrganization(Organization.Info organization) {
      this.organization = organization;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setTrust(float trust) {
      this.trust = trust;
      return this;
    }

    public Builder setType(Type type) {
      this.type = type;
      return this;
    }

    public Builder setFlags(Set<Flag> flags) {
      this.flags = ObjectUtils.ifNotNull(flags, SetUtils::set);
      return this;
    }

    public Builder addFlag(Flag flag) {
      this.flags = SetUtils.addToSet(this.flags, flag);
      return this;
    }
  }

  @Schema(name = "OriginInfo", description = "Short summary of an Origin.")
  public static class Info {
    @Schema(description = "Uniquely identifies the Origin", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
    private final UUID id;
    @Schema(description = "Name of the Origin", example = "John Doe", requiredMode = REQUIRED)
    private final String name;

    private Info(UUID id, String name) {
      this.id = id;
      this.name = name;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }

}
