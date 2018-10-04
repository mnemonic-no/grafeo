package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.*;
import static no.mnemonic.act.platform.dao.cassandra.entity.FactEntity.TABLE;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class FactEntity implements CassandraEntity {

  public static final String TABLE = "fact";

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final ObjectReader reader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, FactObjectBinding.class));
  private static final ObjectWriter writer = mapper.writerFor(mapper.getTypeFactory().constructCollectionType(List.class, FactObjectBinding.class));

  @PartitionKey
  private UUID id;
  @Column(name = "type_id")
  private UUID typeID;
  private String value;
  @Column(name = "in_reference_to_id")
  private UUID inReferenceToID;
  @Column(name = "organization_id")
  private UUID organizationID;
  @Column(name = "source_id")
  private UUID sourceID;
  @Column(name = "access_mode")
  private AccessMode accessMode;
  // TODO: Change to enum after we have defined confidence levels.
  @Column(name = "confidence_level")
  private int confidenceLevel;
  private long timestamp;
  @Column(name = "last_seen_timestamp")
  private long lastSeenTimestamp;
  // In order to not create another table bindings are stored as a JSON string.
  @Column(name = "bindings")
  private String bindingsStored;
  // But they are also available as objects.
  @Transient
  private List<FactObjectBinding> bindings;

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

  public UUID getSourceID() {
    return sourceID;
  }

  public FactEntity setSourceID(UUID sourceID) {
    this.sourceID = sourceID;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public FactEntity setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public int getConfidenceLevel() {
    return confidenceLevel;
  }

  public FactEntity setConfidenceLevel(int confidenceLevel) {
    this.confidenceLevel = confidenceLevel;
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
    } catch (IOException e) {
      throw new RuntimeException(String.format("Could not read 'bindings' for Fact with id = %s.", getId()));
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
    } catch (IOException e) {
      throw new RuntimeException(String.format("Could not write 'bindings' for Fact with id = %s.", getId()));
    }

    return this;
  }

  @Override
  public FactEntity clone() {
    return new FactEntity()
            .setId(getId())
            .setTypeID(getTypeID())
            .setValue(getValue())
            .setInReferenceToID(getInReferenceToID())
            .setOrganizationID(getOrganizationID())
            .setSourceID(getSourceID())
            .setAccessMode(getAccessMode())
            .setConfidenceLevel(getConfidenceLevel())
            .setTimestamp(getTimestamp())
            .setLastSeenTimestamp(getLastSeenTimestamp())
            .setBindingsStored(getBindingsStored());
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
