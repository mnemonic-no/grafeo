package no.mnemonic.act.platform.entity.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Table(
        keyspace = "act",
        name = "source",
        readConsistency = "LOCAL_QUORUM",
        writeConsistency = "LOCAL_QUORUM"
)
public class SourceEntity implements CassandraEntity {

  public enum Type implements CassandraEnum<Type> {
    User(0), InputPort(1), AnalysisModule(2);

    private int value;

    Type(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }

    public static Map<Integer, Type> getValueMap() {
      // After we have moved the *Util classes we can be a little bit smarter here
      // and not generate the map on every access.
      Map<Integer, Type> enumValues = new HashMap<>();
      for (Type type : values()) {
        enumValues.put(type.value(), type);
      }

      return enumValues;
    }
  }

  @PartitionKey
  private UUID id;
  @Column(name = "namespace_id")
  private UUID namespaceID;
  @Column(name = "customer_id")
  private UUID customerID;
  private String name;
  private Type type;
  // TODO: Change to enum after we have defined trust levels.
  @Column(name = "trust_level")
  private int trustLevel;

  public UUID getId() {
    return id;
  }

  public SourceEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getNamespaceID() {
    return namespaceID;
  }

  public SourceEntity setNamespaceID(UUID namespaceID) {
    this.namespaceID = namespaceID;
    return this;
  }

  public UUID getCustomerID() {
    return customerID;
  }

  public SourceEntity setCustomerID(UUID customerID) {
    this.customerID = customerID;
    return this;
  }

  public String getName() {
    return name;
  }

  public SourceEntity setName(String name) {
    this.name = name;
    return this;
  }

  public Type getType() {
    return type;
  }

  public SourceEntity setType(Type type) {
    this.type = type;
    return this;
  }

  public int getTrustLevel() {
    return trustLevel;
  }

  public SourceEntity setTrustLevel(int trustLevel) {
    this.trustLevel = trustLevel;
    return this;
  }

}
