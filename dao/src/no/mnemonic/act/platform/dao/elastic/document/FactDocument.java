package no.mnemonic.act.platform.dao.elastic.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;
import java.util.UUID;

public class FactDocument implements ElasticDocument {

  public enum AccessMode {
    Public, RoleBased, Explicit
  }

  @JsonIgnore // 'id' won't be indexed separately, '_id' is used instead.
  private UUID id;
  private boolean retracted;
  private UUID typeID;
  private String typeName;
  private String value;
  private UUID inReferenceTo;
  private UUID organizationID;
  private String organizationName;
  private UUID sourceID;
  private String sourceName;
  private AccessMode accessMode;
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

  public UUID getSourceID() {
    return sourceID;
  }

  public FactDocument setSourceID(UUID sourceID) {
    this.sourceID = sourceID;
    return this;
  }

  public String getSourceName() {
    return sourceName;
  }

  public FactDocument setSourceName(String sourceName) {
    this.sourceName = sourceName;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public FactDocument setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
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
