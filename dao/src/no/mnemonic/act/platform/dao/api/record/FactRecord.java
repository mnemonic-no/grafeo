package no.mnemonic.act.platform.dao.api.record;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Record representing a Fact.
 */
public class FactRecord {

  public enum AccessMode {
    Public, RoleBased, Explicit
  }

  public enum Flag {
    /**
     * The 'RetractedHint' flag only indicates that the Fact has been retracted by someone. From the user's point of view
     * the Fact is only retracted if one has access to the retraction Fact. This must be checked in the service implementation.
     */
    RetractedHint
  }

  private UUID id;
  private UUID typeID;
  private String value;
  private UUID inReferenceToID;
  private UUID organizationID;
  private UUID originID;
  private UUID addedByID;
  private AccessMode accessMode;
  private float confidence;
  private float trust;
  private long timestamp;
  private long lastSeenTimestamp;
  private ObjectRecord sourceObject;
  private ObjectRecord destinationObject;
  private boolean bidirectionalBinding;
  private Set<Flag> flags;
  private List<FactAclEntryRecord> acl;
  private List<FactCommentRecord> comments;

  public UUID getId() {
    return id;
  }

  public FactRecord setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getTypeID() {
    return typeID;
  }

  public FactRecord setTypeID(UUID typeID) {
    this.typeID = typeID;
    return this;
  }

  public String getValue() {
    return value;
  }

  public FactRecord setValue(String value) {
    this.value = value;
    return this;
  }

  public UUID getInReferenceToID() {
    return inReferenceToID;
  }

  public FactRecord setInReferenceToID(UUID inReferenceToID) {
    this.inReferenceToID = inReferenceToID;
    return this;
  }

  public UUID getOrganizationID() {
    return organizationID;
  }

  public FactRecord setOrganizationID(UUID organizationID) {
    this.organizationID = organizationID;
    return this;
  }

  public UUID getOriginID() {
    return originID;
  }

  public FactRecord setOriginID(UUID originID) {
    this.originID = originID;
    return this;
  }

  public UUID getAddedByID() {
    return addedByID;
  }

  public FactRecord setAddedByID(UUID addedByID) {
    this.addedByID = addedByID;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public FactRecord setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public float getConfidence() {
    return confidence;
  }

  public FactRecord setConfidence(float confidence) {
    this.confidence = confidence;
    return this;
  }

  public float getTrust() {
    return trust;
  }

  public FactRecord setTrust(float trust) {
    this.trust = trust;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactRecord setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public FactRecord setLastSeenTimestamp(long lastSeenTimestamp) {
    this.lastSeenTimestamp = lastSeenTimestamp;
    return this;
  }

  public ObjectRecord getSourceObject() {
    return sourceObject;
  }

  public FactRecord setSourceObject(ObjectRecord sourceObject) {
    this.sourceObject = sourceObject;
    return this;
  }

  public ObjectRecord getDestinationObject() {
    return destinationObject;
  }

  public FactRecord setDestinationObject(ObjectRecord destinationObject) {
    this.destinationObject = destinationObject;
    return this;
  }

  public boolean isBidirectionalBinding() {
    return bidirectionalBinding;
  }

  public FactRecord setBidirectionalBinding(boolean bidirectionalBinding) {
    this.bidirectionalBinding = bidirectionalBinding;
    return this;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public FactRecord setFlags(Set<Flag> flags) {
    this.flags = flags;
    return this;
  }

  public FactRecord addFlag(Flag flag) {
    this.flags = SetUtils.addToSet(this.flags, flag);
    return this;
  }

  public List<FactAclEntryRecord> getAcl() {
    return acl;
  }

  public FactRecord setAcl(List<FactAclEntryRecord> acl) {
    this.acl = acl;
    return this;
  }

  public FactRecord addAclEntry(FactAclEntryRecord entry) {
    this.acl = ListUtils.addToList(this.acl, entry);
    return this;
  }

  public List<FactCommentRecord> getComments() {
    return comments;
  }

  public FactRecord setComments(List<FactCommentRecord> comments) {
    this.comments = comments;
    return this;
  }

  public FactRecord addComment(FactCommentRecord comment) {
    this.comments = ListUtils.addToList(this.comments, comment);
    return this;
  }
}
