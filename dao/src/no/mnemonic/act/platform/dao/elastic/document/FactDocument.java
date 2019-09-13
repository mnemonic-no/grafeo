package no.mnemonic.act.platform.dao.elastic.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

public class FactDocument implements ElasticDocument {

  public enum AccessMode {
    Public, RoleBased, Explicit
  }

  public static final float DEFAULT_CONFIDENCE = 1.0f;
  public static final float DEFAULT_TRUST = 0.8f;

  @JsonIgnore // 'id' won't be indexed separately, '_id' is used instead.
  private UUID id;
  private boolean retracted;
  private UUID typeID;
  private String typeName;
  private String value;
  private UUID inReferenceTo;
  private UUID organizationID;
  private String organizationName;
  @JsonProperty("sourceID")
  private UUID originID;
  @JsonProperty("sourceName")
  private String originName;
  private UUID addedByID;
  private String addedByName;
  private AccessMode accessMode;
  private Float confidence;
  private Float trust;
  private long timestamp;
  private long lastSeenTimestamp;
  private Set<UUID> acl;
  private Set<ObjectDocument> objects;

  public UUID getId() {
    return id;
  }

  public FactDocument setId(UUID id) {
    this.id = id;
    return this;
  }

  public boolean isRetracted() {
    return retracted;
  }

  public FactDocument setRetracted(boolean retracted) {
    this.retracted = retracted;
    return this;
  }

  public UUID getTypeID() {
    return typeID;
  }

  public FactDocument setTypeID(UUID typeID) {
    this.typeID = typeID;
    return this;
  }

  public String getTypeName() {
    return typeName;
  }

  public FactDocument setTypeName(String typeName) {
    this.typeName = typeName;
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

  public String getOrganizationName() {
    return organizationName;
  }

  public FactDocument setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
    return this;
  }

  public UUID getOriginID() {
    return originID;
  }

  public FactDocument setOriginID(UUID originID) {
    this.originID = originID;
    return this;
  }

  public String getOriginName() {
    return originName;
  }

  public FactDocument setOriginName(String originName) {
    this.originName = originName;
    return this;
  }

  public UUID getAddedByID() {
    return addedByID;
  }

  public FactDocument setAddedByID(UUID addedByID) {
    this.addedByID = addedByID;
    return this;
  }

  public String getAddedByName() {
    return addedByName;
  }

  public FactDocument setAddedByName(String addedByName) {
    this.addedByName = addedByName;
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
