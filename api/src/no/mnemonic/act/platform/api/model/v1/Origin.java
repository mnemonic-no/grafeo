package no.mnemonic.act.platform.api.model.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@ApiModel(description = "Every piece of information (i.e. Facts) put into the system is marked with an Origin " +
        "in order to identify where the information came from."
)
public class Origin {

  public enum Type {
    Group, User
  }

  public enum Flag {
    Deleted
  }

  @ApiModelProperty(value = "Uniquely identifies the Origin", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Namespace the Origin belongs to", required = true)
  private final Namespace namespace;
  @ApiModelProperty(value = "Organization the Origin belongs to")
  private final Organization.Info organization;
  @ApiModelProperty(value = "Name of the Origin", example = "John Doe", required = true)
  private final String name;
  @ApiModelProperty(value = "Longer description about the Origin", example = "John Doe from Doe Inc")
  private final String description;
  @ApiModelProperty(value = "How much the Origin is trusted", example = "0.8", required = true)
  private final float trust;
  @ApiModelProperty(value = "Type of the Origin (group or individual user)", required = true)
  private final Type type;
  @ApiModelProperty(value = "Contains any flags set on the Origin")
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

  @ApiModel(value = "OriginInfo", description = "Short summary of an Origin.")
  public static class Info {
    @ApiModelProperty(value = "Uniquely identifies the Origin", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
    private final UUID id;
    @ApiModelProperty(value = "Name of the Origin", example = "John Doe", required = true)
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
