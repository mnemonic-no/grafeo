package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.mnemonic.act.platform.utilities.json.RoundingFloatDeserializer;
import no.mnemonic.act.platform.utilities.json.RoundingFloatSerializer;
import no.mnemonic.act.platform.utilities.json.TimestampDeserializer;
import no.mnemonic.act.platform.utilities.json.TimestampSerializer;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@JsonDeserialize(builder = FactSEB.Builder.class)
public class FactSEB {

  public enum AccessMode {
    Public, RoleBased, Explicit
  }

  public enum Flag {
    RetractedHint
  }

  private final UUID id;
  private final FactTypeInfoSEB type;
  private final String value;
  private final FactInfoSEB inReferenceTo;
  private final OrganizationInfoSEB organization;
  private final OriginInfoSEB origin;
  private final SubjectInfoSEB addedBy;
  private final AccessMode accessMode;
  @JsonSerialize(using = RoundingFloatSerializer.class)
  private final float trust;
  @JsonSerialize(using = RoundingFloatSerializer.class)
  private final float confidence;
  @JsonSerialize(using = TimestampSerializer.class)
  private final long timestamp;
  @JsonSerialize(using = TimestampSerializer.class)
  private final long lastSeenTimestamp;
  private final ObjectInfoSEB sourceObject;
  private final ObjectInfoSEB destinationObject;
  private final boolean bidirectionalBinding;
  private final Set<Flag> flags;
  private final Set<AclEntrySEB> acl;

  private FactSEB(UUID id,
                  FactTypeInfoSEB type,
                  String value,
                  FactInfoSEB inReferenceTo,
                  OrganizationInfoSEB organization,
                  OriginInfoSEB origin,
                  SubjectInfoSEB addedBy,
                  AccessMode accessMode,
                  float trust,
                  float confidence,
                  long timestamp,
                  long lastSeenTimestamp,
                  ObjectInfoSEB sourceObject,
                  ObjectInfoSEB destinationObject,
                  boolean bidirectionalBinding,
                  Set<Flag> flags,
                  Set<AclEntrySEB> acl) {
    this.id = id;
    this.type = type;
    this.value = value;
    this.inReferenceTo = inReferenceTo;
    this.organization = organization;
    this.origin = origin;
    this.addedBy = addedBy;
    this.accessMode = accessMode;
    this.trust = trust;
    this.confidence = confidence;
    this.timestamp = timestamp;
    this.lastSeenTimestamp = lastSeenTimestamp;
    this.sourceObject = sourceObject;
    this.destinationObject = destinationObject;
    this.bidirectionalBinding = bidirectionalBinding;
    this.flags = ObjectUtils.ifNotNull(flags, Collections::unmodifiableSet);
    this.acl = ObjectUtils.ifNotNull(acl, Collections::unmodifiableSet);
  }

  public UUID getId() {
    return id;
  }

  public FactTypeInfoSEB getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public FactInfoSEB getInReferenceTo() {
    return inReferenceTo;
  }

  public OrganizationInfoSEB getOrganization() {
    return organization;
  }

  public OriginInfoSEB getOrigin() {
    return origin;
  }

  public SubjectInfoSEB getAddedBy() {
    return addedBy;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public float getTrust() {
    return trust;
  }

  public float getConfidence() {
    return confidence;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public ObjectInfoSEB getSourceObject() {
    return sourceObject;
  }

  public ObjectInfoSEB getDestinationObject() {
    return destinationObject;
  }

  public boolean isBidirectionalBinding() {
    return bidirectionalBinding;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public Set<AclEntrySEB> getAcl() {
    return acl;
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "set")
  public static class Builder {
    private UUID id;
    private FactTypeInfoSEB type;
    private String value;
    private FactInfoSEB inReferenceTo;
    private OrganizationInfoSEB organization;
    private OriginInfoSEB origin;
    private SubjectInfoSEB addedBy;
    private AccessMode accessMode;
    @JsonDeserialize(using = RoundingFloatDeserializer.class)
    private float trust;
    @JsonDeserialize(using = RoundingFloatDeserializer.class)
    private float confidence;
    @JsonDeserialize(using = TimestampDeserializer.class)
    private long timestamp;
    @JsonDeserialize(using = TimestampDeserializer.class)
    private long lastSeenTimestamp;
    private ObjectInfoSEB sourceObject;
    private ObjectInfoSEB destinationObject;
    private boolean bidirectionalBinding;
    private Set<Flag> flags;
    private Set<AclEntrySEB> acl;

    private Builder() {
    }

    public FactSEB build() {
      return new FactSEB(id, type, value, inReferenceTo, organization, origin, addedBy, accessMode, trust, confidence,
              timestamp, lastSeenTimestamp, sourceObject, destinationObject, bidirectionalBinding, flags, acl);
    }

    public Builder setId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder setType(FactTypeInfoSEB type) {
      this.type = type;
      return this;
    }

    public Builder setValue(String value) {
      this.value = value;
      return this;
    }

    public Builder setInReferenceTo(FactInfoSEB inReferenceTo) {
      this.inReferenceTo = inReferenceTo;
      return this;
    }

    public Builder setOrganization(OrganizationInfoSEB organization) {
      this.organization = organization;
      return this;
    }

    public Builder setOrigin(OriginInfoSEB origin) {
      this.origin = origin;
      return this;
    }

    public Builder setAddedBy(SubjectInfoSEB addedBy) {
      this.addedBy = addedBy;
      return this;
    }

    public Builder setAccessMode(AccessMode accessMode) {
      this.accessMode = accessMode;
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

    public Builder setTimestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder setLastSeenTimestamp(long lastSeenTimestamp) {
      this.lastSeenTimestamp = lastSeenTimestamp;
      return this;
    }

    public Builder setSourceObject(ObjectInfoSEB sourceObject) {
      this.sourceObject = sourceObject;
      return this;
    }

    public Builder setDestinationObject(ObjectInfoSEB destinationObject) {
      this.destinationObject = destinationObject;
      return this;
    }

    public Builder setBidirectionalBinding(boolean bidirectionalBinding) {
      this.bidirectionalBinding = bidirectionalBinding;
      return this;
    }

    public Builder setFlags(Set<Flag> flags) {
      this.flags = flags;
      return this;
    }

    public Builder addFlag(Flag flag) {
      this.flags = SetUtils.addToSet(this.flags, flag);
      return this;
    }

    public Builder setAcl(Set<AclEntrySEB> acl) {
      this.acl = acl;
      return this;
    }

    public Builder addAclEntry(AclEntrySEB entry) {
      this.acl = SetUtils.addToSet(this.acl, entry);
      return this;
    }
  }
}
