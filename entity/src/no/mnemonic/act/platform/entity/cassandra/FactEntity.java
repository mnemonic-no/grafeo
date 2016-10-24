package no.mnemonic.act.platform.entity.cassandra;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Table(
        keyspace = "act",
        name = "fact",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class FactEntity implements CassandraEntity {

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
  @Column(name = "customer_id")
  private UUID customerID;
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

  public UUID getCustomerID() {
    return customerID;
  }

  public FactEntity setCustomerID(UUID customerID) {
    this.customerID = customerID;
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

  public FactEntity setBindingsStored(String bindingsStored) throws IOException {
    this.bindingsStored = bindingsStored;
    this.bindings = reader.readValue(bindingsStored);
    return this;
  }

  public List<FactObjectBinding> getBindings() {
    return bindings;
  }

  public FactEntity setBindings(List<FactObjectBinding> bindings) throws IOException {
    this.bindings = bindings;
    this.bindingsStored = writer.writeValueAsString(bindings);
    return this;
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
      this.direction = Direction.getValueMap().getOrDefault(value, Direction.None);
      return this;
    }
  }

}
