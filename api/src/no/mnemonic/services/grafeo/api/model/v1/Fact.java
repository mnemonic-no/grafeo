package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.utilities.json.RoundingFloatSerializer;
import no.mnemonic.services.grafeo.utilities.json.TimestampSerializer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Facts provide additional information about an Object. " +
        "One Fact can link one or two Objects together and thereby describes the relationship between those Objects."
)
public class Fact {

  public enum Flag {
    Retracted
  }

  @Schema(description = "Uniquely identifies the Fact", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
  private final UUID id;
  @Schema(description = "Type of the Fact", requiredMode = REQUIRED)
  private final FactType.Info type;
  @Schema(description = "Contains the actual information", example = "APT1")
  private final String value;
  @Schema(description = "Links directly to another Fact")
  private final Fact.Info inReferenceTo;
  @Schema(description = "Who owns the Fact", requiredMode = REQUIRED)
  private final Organization.Info organization;
  @Schema(description = "Who added the Fact", requiredMode = REQUIRED)
  private final Subject.Info addedBy;
  @Schema(description = "Who saw the Fact last", requiredMode = REQUIRED)
  private final Subject.Info lastSeenBy;
  @Schema(description = "Where the information came from", requiredMode = REQUIRED)
  private final Origin.Info origin;
  @Schema(description = "How much the Origin was trusted when the Fact was created", example = "0.8", requiredMode = REQUIRED)
  @JsonSerialize(using = RoundingFloatSerializer.class)
  private final float trust;
  @Schema(description = "How much confidence was given that the Fact is true", example = "0.9", requiredMode = REQUIRED)
  @JsonSerialize(using = RoundingFloatSerializer.class)
  private final float confidence;
  @Schema(description = "Who has access to the Fact", requiredMode = REQUIRED)
  private final AccessMode accessMode;
  @Schema(description = "When the Fact was created", example = "2016-09-28T21:26:22Z", type = "string", requiredMode = REQUIRED)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long timestamp;
  @Schema(description = "When the Fact was last seen", example = "2016-09-28T21:26:22Z", type = "string", requiredMode = REQUIRED)
  @JsonSerialize(using = TimestampSerializer.class)
  private final Long lastSeenTimestamp;
  @Schema(description = "Object which is linked to Fact as source")
  private final Object.Info sourceObject;
  @Schema(description = "Object which is linked to Fact as destination")
  private final Object.Info destinationObject;
  @Schema(description = "True if the binding between source Object, Fact and destination Object is bidirectional", requiredMode = REQUIRED)
  private final boolean bidirectionalBinding;
  @Schema(description = "Contains any flags set on the Fact")
  private final Set<Flag> flags;

  private Fact(UUID id,
               FactType.Info type,
               String value,
               Info inReferenceTo,
               Organization.Info organization,
               Subject.Info addedBy,
               Subject.Info lastSeenBy,
               Origin.Info origin,
               float trust,
               float confidence,
               AccessMode accessMode,
               Long timestamp,
               Long lastSeenTimestamp,
               Object.Info sourceObject,
               Object.Info destinationObject,
               boolean bidirectionalBinding,
               Set<Flag> flags) {
    this.id = id;
    this.type = type;
    this.value = value;
    this.inReferenceTo = inReferenceTo;
    this.organization = organization;
    this.addedBy = addedBy;
    this.lastSeenBy = lastSeenBy;
    this.origin = origin;
    this.trust = trust;
    this.confidence = confidence;
    this.accessMode = accessMode;
    this.timestamp = timestamp;
    this.lastSeenTimestamp = lastSeenTimestamp;
    this.sourceObject = sourceObject;
    this.destinationObject = destinationObject;
    this.bidirectionalBinding = bidirectionalBinding;
    this.flags = ObjectUtils.ifNotNull(flags, Collections::unmodifiableSet);
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

  public Subject.Info getAddedBy() {
    return addedBy;
  }

  public Subject.Info getLastSeenBy() {
    return lastSeenBy;
  }

  public Origin.Info getOrigin() {
    return origin;
  }

  public float getTrust() {
    return trust;
  }

  public float getConfidence() {
    return confidence;
  }

  @Schema(description = "Certainty = Trust x Confidence", example = "0.72", requiredMode = REQUIRED)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JsonSerialize(using = RoundingFloatSerializer.class)
  public float getCertainty() {
    return getTrust() * getConfidence();
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

  public Object.Info getSourceObject() {
    return sourceObject;
  }

  public Object.Info getDestinationObject() {
    return destinationObject;
  }

  public boolean isBidirectionalBinding() {
    return bidirectionalBinding;
  }

  public Set<Flag> getFlags() {
    return flags;
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
    private Subject.Info addedBy;
    private Subject.Info lastSeenBy;
    private Origin.Info origin;
    private float trust;
    private float confidence;
    private AccessMode accessMode;
    private Long timestamp;
    private Long lastSeenTimestamp;
    private Object.Info sourceObject;
    private Object.Info destinationObject;
    private boolean bidirectionalBinding;
    private Set<Flag> flags;

    private Builder() {
    }

    public Fact build() {
      return new Fact(id, type, value, inReferenceTo, organization, addedBy, lastSeenBy, origin, trust, confidence,
              accessMode, timestamp, lastSeenTimestamp, sourceObject, destinationObject, bidirectionalBinding, flags);
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

    public Builder setAddedBy(Subject.Info addedBy) {
      this.addedBy = addedBy;
      return this;
    }

    public Builder setLastSeenBy(Subject.Info lastSeenBy) {
      this.lastSeenBy = lastSeenBy;
      return this;
    }

    public Builder setOrigin(Origin.Info origin) {
      this.origin = origin;
      return this;
    }

    public Builder setTrust(float trust) {
      this.trust = trust;
      return this;
    }

    public Builder setConfidence(float confidence) {
      this.confidence = confidence;
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

    public Builder setSourceObject(Object.Info sourceObject) {
      this.sourceObject = sourceObject;
      return this;
    }

    public Builder setDestinationObject(Object.Info destinationObject) {
      this.destinationObject = destinationObject;
      return this;
    }

    public Builder setBidirectionalBinding(boolean bidirectionalBinding) {
      this.bidirectionalBinding = bidirectionalBinding;
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

  @Schema(name = "FactInfo", description = "Short summary of a Fact.")
  public static class Info {
    @Schema(description = "Uniquely identifies the Fact", example = "123e4567-e89b-12d3-a456-426655440000", requiredMode = REQUIRED)
    private final UUID id;
    @Schema(description = "Type of the Fact", requiredMode = REQUIRED)
    private final FactType.Info type;
    @Schema(description = "Contains the actual information", example = "APT1")
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
