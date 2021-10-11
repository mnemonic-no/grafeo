package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.datastax.oss.driver.api.mapper.annotations.Transient;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;
import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactEntity.TABLE;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class FactEntity implements CassandraEntity {

  public static final String TABLE = "fact";
  public static final float DEFAULT_CONFIDENCE = 1.0f;
  public static final float DEFAULT_TRUST = 0.8f;

  private static final ObjectMapper mapper = JsonMapper.builder().build();
  private static final ObjectReader reader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, FactObjectBinding.class));
  private static final ObjectWriter writer = mapper.writerFor(mapper.getTypeFactory().constructCollectionType(List.class, FactObjectBinding.class));
  private static final Logger logger = Logging.getLogger(FactEntity.class);

  public enum Flag implements CassandraEnum<Flag> {
    RetractedHint(0, false),
    HasAcl(1, true),
    HasComments(2, true),
    BidirectionalBinding(3, true),
    // Flag which indicates that the 'source_object_id' and 'destination_object_id' fields are used instead of 'bindings'.
    // 'bindings' is deprecated but kept for backwards-compatibility. It will be removed in the future.
    UsesSeparatedObjectFields(4, true);

    private static final Map<Integer, Flag> enumValues = unmodifiableMap(map(v -> T(v.value(), v), values()));
    private final int value;
    private final boolean cassandraOnly;

    Flag(int value, boolean cassandraOnly) {
      this.value = value;
      this.cassandraOnly = cassandraOnly;
    }

    @Override
    public int value() {
      return value;
    }

    public boolean isCassandraOnly() {
      return cassandraOnly;
    }

    public static Map<Integer, Flag> getValueMap() {
      return enumValues;
    }
  }

  @PartitionKey
  private UUID id;
  @CqlName("type_id")
  private UUID typeID;
  private String value;
  @CqlName("in_reference_to_id")
  private UUID inReferenceToID;
  @CqlName("organization_id")
  private UUID organizationID;
  @CqlName("source_id")
  private UUID originID;
  @CqlName("added_by_id")
  private UUID addedByID;
  @CqlName("access_mode")
  private AccessMode accessMode;
  private Float confidence;
  private Float trust;
  private long timestamp;
  @CqlName("last_seen_timestamp")
  private long lastSeenTimestamp;
  // In order to not create another table bindings are stored as a JSON string.
  @CqlName("bindings")
  private String bindingsStored;
  // But they are also available as objects.
  @Transient
  private List<FactObjectBinding> bindings;
  @CqlName("source_object_id")
  private UUID sourceObjectID;
  @CqlName("destination_object_id")
  private UUID destinationObjectID;
  private Set<Flag> flags;

  public UUID getId() {
    return id;
  }

  public FactEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getTypeID() {
    return typeID;
  }

  public FactEntity setTypeID(UUID typeID) {
    this.typeID = typeID;
    return this;
  }

  public String getValue() {
    return value;
  }

  public FactEntity setValue(String value) {
    this.value = value;
    return this;
  }

  public UUID getInReferenceToID() {
    return inReferenceToID;
  }

  public FactEntity setInReferenceToID(UUID inReferenceToID) {
    this.inReferenceToID = inReferenceToID;
    return this;
  }

  public UUID getOrganizationID() {
    return organizationID;
  }

  public FactEntity setOrganizationID(UUID organizationID) {
    this.organizationID = organizationID;
    return this;
  }

  public UUID getOriginID() {
    return originID;
  }

  public FactEntity setOriginID(UUID originID) {
    this.originID = originID;
    return this;
  }

  public UUID getAddedByID() {
    return addedByID;
  }

  public FactEntity setAddedByID(UUID addedByID) {
    this.addedByID = addedByID;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public FactEntity setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public Float getConfidence() {
    // Required for backwards-compatibility where the 'confidence' column is unset.
    return ObjectUtils.ifNull(confidence, DEFAULT_CONFIDENCE);
  }

  public FactEntity setConfidence(Float confidence) {
    this.confidence = confidence;
    return this;
  }

  public Float getTrust() {
    // Required for backwards-compatibility where the 'trust' column is unset.
    return ObjectUtils.ifNull(trust, DEFAULT_TRUST);
  }

  public FactEntity setTrust(Float trust) {
    this.trust = trust;
    return this;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public FactEntity setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public long getLastSeenTimestamp() {
    return lastSeenTimestamp;
  }

  public FactEntity setLastSeenTimestamp(long lastSeenTimestamp) {
    this.lastSeenTimestamp = lastSeenTimestamp;
    return this;
  }

  public String getBindingsStored() {
    return bindingsStored;
  }

  public FactEntity setBindingsStored(String bindingsStored) {
    this.bindingsStored = bindingsStored;

    try {
      this.bindings = !StringUtils.isBlank(bindingsStored) ? reader.readValue(bindingsStored) : null;
    } catch (IOException ex) {
      logAndRethrow(ex, String.format("Could not read 'bindings' for Fact with id = %s.", getId()));
    }

    return this;
  }

  public List<FactObjectBinding> getBindings() {
    return bindings;
  }

  public FactEntity setBindings(List<FactObjectBinding> bindings) {
    this.bindings = bindings;

    try {
      this.bindingsStored = !CollectionUtils.isEmpty(bindings) ? writer.writeValueAsString(bindings) : null;
    } catch (IOException ex) {
      logAndRethrow(ex, String.format("Could not write 'bindings' for Fact with id = %s.", getId()));
    }

    return this;
  }

  public FactEntity addBinding(FactObjectBinding binding) {
    // Need to call setBindings() in order to store JSON blob.
    return setBindings(ListUtils.addToList(bindings, binding));
  }

  public UUID getSourceObjectID() {
    return sourceObjectID;
  }

  public FactEntity setSourceObjectID(UUID sourceObjectID) {
    this.sourceObjectID = sourceObjectID;
    return this;
  }

  public UUID getDestinationObjectID() {
    return destinationObjectID;
  }

  public FactEntity setDestinationObjectID(UUID destinationObjectID) {
    this.destinationObjectID = destinationObjectID;
    return this;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public FactEntity setFlags(Set<Flag> flags) {
    this.flags = flags;
    return this;
  }

  public FactEntity addFlag(Flag flag) {
    this.flags = SetUtils.addToSet(this.flags, flag);
    return this;
  }

  public boolean isSet(Flag flag) {
    return SetUtils.set(flags).contains(flag);
  }

  private void logAndRethrow(IOException ex, String msg) {
    logger.error(ex, msg);
    throw new UncheckedIOException(msg, ex);
  }

  public static class FactObjectBinding {

    private UUID objectID;
    @JsonIgnore
    private Direction direction;

    public UUID getObjectID() {
      return objectID;
    }

    public FactObjectBinding setObjectID(UUID objectID) {
      this.objectID = objectID;
      return this;
    }

    public Direction getDirection() {
      return direction;
    }

    public FactObjectBinding setDirection(Direction direction) {
      this.direction = direction;
      return this;
    }

    @JsonGetter("direction")
    public int getDirectionValue() {
      return direction != null ? direction.value() : 0;
    }

    @JsonSetter("direction")
    public FactObjectBinding setDirectionValue(int value) {
      this.direction = Direction.getValueMap().get(value);
      return this;
    }
  }

}
