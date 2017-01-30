package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.json.TimestampSerializer;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Facts provide additional information about an Object. " +
        "One Fact can link two or more Objects together and thereby describes the relationship between those Objects."
)
public class Fact {

  @ApiModelProperty(value = "Uniquely identifies the Fact", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
  private final UUID id;
  @ApiModelProperty(value = "Type of the Fact", required = true)
  private final FactType.Info type;
  @ApiModelProperty(value = "Contains the actual information. Encoding depends on the used FactType", example = "APT1", required = true)
  private final String value;
  @ApiModelProperty(value = "Links directly to another Fact")
  private final Fact.Info inReferenceTo;
  @ApiModelProperty(value = "Who owns the Fact", required = true)
  private final Organization.Info organization;
  @ApiModelProperty(value = "Who created the Fact", required = true)
  private final Source.Info source;
  @ApiModelProperty(value = "Who has access to the Fact", required = true)
  private final AccessMode accessMode;
  @ApiModelProperty(value = "When the Fact was created", example = "2016-09-28T21:26:22", dataType = "string", required = true)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;
  @ApiModelProperty(value = "When the Fact was last seen", example = "2016-09-28T21:26:22", dataType = "string", required = true)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long lastSeenTimestamp;
  @ApiModelProperty(value = "Links the Fact to Objects")
  private final List<FactObjectBinding> objects;
  // TODO: Add confidenceLevel once defined.

  private Fact(UUID id, FactType.Info type, String value, Info inReferenceTo, Organization.Info organization, Source.Info source,
               AccessMode accessMode, Long timestamp, Long lastSeenTimestamp, List<FactObjectBinding> objects) {
    this.id = id;
    this.type = type;
    this.value = value;
    this.inReferenceTo = inReferenceTo;
    this.organization = organization;
    this.source = source;
    this.accessMode = accessMode;
    this.timestamp = timestamp;
    this.lastSeenTimestamp = lastSeenTimestamp;
    this.objects = ObjectUtils.ifNotNull(objects, Collections::unmodifiableList);
  }

  public UUID getId() {
    return id;
  }

  public FactType.Info getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public Info getInReferenceTo() {
    return inReferenceTo;
  }

  public Organization.Info getOrganization() {
    return organization;
  }

  public Source.Info getSource() {
    return source;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public Long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public List<FactObjectBinding> getObjects() {
    return objects;
  }

  public Info toInfo() {
    return new Info(id, type, value);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private FactType.Info type;
    private String value;
    private Fact.Info inReferenceTo;
    private Organization.Info organization;
    private Source.Info source;
    private AccessMode accessMode;
    private Long timestamp;
    private Long lastSeenTimestamp;
    private List<FactObjectBinding> objects;

    private Builder() {
    }

    public Fact build() {
      return new Fact(id, type, value, inReferenceTo, organization, source, accessMode, timestamp, lastSeenTimestamp, objects);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setType(FactType.Info type) {
      this.type = type;
      return this;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public Builder setInReferenceTo(Info inReferenceTo) {
      this.inReferenceTo = inReferenceTo;
      return this;
    }

    public Builder setOrganization(Organization.Info organization) {
      this.organization = organization;
      return this;
    }

    public Builder setSource(Source.Info source) {
      this.source = source;
      return this;
    }

    public Builder setAccessMode(AccessMode accessMode) {
      this.accessMode = accessMode;
      return this;
    }

    public Builder setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder setLastSeenTimestamp(Long lastSeenTimestamp) {
      this.lastSeenTimestamp = lastSeenTimestamp;
      return this;
    }

    public Builder setObjects(List<FactObjectBinding> objects) {
      this.objects = objects;
      return this;
    }

    public Builder addObject(FactObjectBinding object) {
      this.objects = ListUtils.addToList(this.objects, object);
      return this;
    }
  }

  @ApiModel(value = "FactObjectBindingModel", description = "Links a Fact to Objects.")
  public static class FactObjectBinding {
    @ApiModelProperty(value = "Linked Object", required = true)
    private final Object.Info object;
    @ApiModelProperty(value = "Direction of the link", required = true)
    private final Direction direction;

    public FactObjectBinding(Object.Info object, Direction direction) {
      this.object = object;
      this.direction = direction;
    }

    public Object.Info getObject() {
      return object;
    }

    public Direction getDirection() {
      return direction;
    }
  }

  @ApiModel(value = "FactInfo", description = "Short summary of a Fact.")
  public class Info {
    @ApiModelProperty(value = "Uniquely identifies the Fact", example = "123e4567-e89b-12d3-a456-426655440000", required = true)
    private final UUID id;
    @ApiModelProperty(value = "Type of the Fact", required = true)
    private final FactType.Info type;
    @ApiModelProperty(value = "Contains the actual information", example = "APT1", required = true)
    private final String value;

    private Info(UUID id, FactType.Info type, String value) {
      this.id = id;
      this.type = type;
      this.value = value;
    }

    public UUID getId() {
      return id;
    }

    public FactType.Info getType() {
      return type;
    }

    public String getValue() {
      return value;
    }
  }

}
