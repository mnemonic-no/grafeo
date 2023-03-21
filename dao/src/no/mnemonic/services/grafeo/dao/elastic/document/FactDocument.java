package no.mnemonic.services.grafeo.dao.elastic.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true) // Required for backwards compatibility.
public class FactDocument implements ElasticDocument {

  public enum AccessMode {
    Public, RoleBased, Explicit
  }

  public enum Flag {
    RetractedHint, TimeGlobalIndex
  }

  public static final float DEFAULT_CONFIDENCE = 1.0f;
  public static final float DEFAULT_TRUST = 0.8f;

  private UUID id;
  private UUID typeID;
  private String value;
  private UUID inReferenceTo;
  private UUID organizationID;
  @JsonProperty("sourceID")
  private UUID originID;
  private UUID addedByID;
  private UUID lastSeenByID;
  private AccessMode accessMode;
  private Float confidence;
  private Float trust;
  private long timestamp;
  private long lastSeenTimestamp;
  private Set<UUID> acl;
  private Set<Flag> flags;
  private Set<ObjectDocument> objects;

  public UUID getId() {
    return id;
  }

  public FactDocument setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getTypeID() {
    return typeID;
  }

  public FactDocument setTypeID(UUID typeID) {
    this.typeID = typeID;
    return this;
  }

  public String getValue() {
    return value;
  }

  public FactDocument setValue(String value) {
    this.value = value;
    return this;
  }

  public UUID getInReferenceTo() {
    return inReferenceTo;
  }

  public FactDocument setInReferenceTo(UUID inReferenceTo) {
    this.inReferenceTo = inReferenceTo;
    return this;
  }

  public UUID getOrganizationID() {
    return organizationID;
  }

  public FactDocument setOrganizationID(UUID organizationID) {
    this.organizationID = organizationID;
    return this;
  }

  public UUID getOriginID() {
    return originID;
  }

  public FactDocument setOriginID(UUID originID) {
    this.originID = originID;
    return this;
  }

  public UUID getAddedByID() {
    return addedByID;
  }

  public FactDocument setAddedByID(UUID addedByID) {
    this.addedByID = addedByID;
    return this;
  }

  public UUID getLastSeenByID() {
    return lastSeenByID;
  }

  public FactDocument setLastSeenByID(UUID lastSeenByID) {
    this.lastSeenByID = lastSeenByID;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public FactDocument setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public Float getConfidence() {
    // Required for backwards-compatibility where the 'confidence' field is unset.
    return ObjectUtils.ifNull(confidence, DEFAULT_CONFIDENCE);
  }

  public FactDocument setConfidence(Float confidence) {
    this.confidence = confidence;
    return this;
  }

  public Float getTrust() {
    // Required for backwards-compatibility where the 'trust' field is unset.
    return ObjectUtils.ifNull(trust, DEFAULT_TRUST);
  }

  public FactDocument setTrust(Float trust) {
    this.trust = trust;
    return this;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public float getCertainty() {
    // This field is only required when storing the document into ElasticSearch in
    // order to have 'certainty' available as a de-normalized field during search.
    float certainty = getConfidence() * getTrust();
    // Round 'certainty' to two decimal points.
    return BigDecimal.valueOf(certainty)
            .setScale(2, RoundingMode.HALF_UP)
            .floatValue();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactDocument setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public FactDocument setLastSeenTimestamp(long lastSeenTimestamp) {
    this.lastSeenTimestamp = lastSeenTimestamp;
    return this;
  }

  public Set<UUID> getAcl() {
    return acl;
  }

  public FactDocument setAcl(Set<UUID> acl) {
    this.acl = acl;
    return this;
  }

  public FactDocument addAclEntry(UUID entry) {
    this.acl = SetUtils.addToSet(this.acl, entry);
    return this;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public FactDocument setFlags(Set<Flag> flags) {
    this.flags = flags;
    return this;
  }

  public FactDocument addFlag(Flag flag) {
    this.flags = SetUtils.addToSet(this.flags, flag);
    return this;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public int getObjectCount() {
    // This field is only required when storing the document into ElasticSearch in
    // order to have 'objectCount' available as a de-normalized field during search.
    return CollectionUtils.size(objects);
  }

  public Set<ObjectDocument> getObjects() {
    return objects;
  }

  public FactDocument setObjects(Set<ObjectDocument> objects) {
    this.objects = objects;
    return this;
  }

  public FactDocument addObject(ObjectDocument object) {
    this.objects = SetUtils.addToSet(this.objects, object);
    return this;
  }

}
