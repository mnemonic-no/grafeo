package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Map;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;
import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.*;
import static no.mnemonic.act.platform.dao.cassandra.entity.SourceEntity.TABLE;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

@Table(
        keyspace = KEY_SPACE,
        name = TABLE,
        readConsistency = READ_CONSISTENCY,
        writeConsistency = WRITE_CONSISTENCY
)
public class SourceEntity implements CassandraEntity {

  public static final String TABLE = "source";

  public enum Type implements CassandraEnum<Type> {
    User(0), InputPort(1), AnalysisModule(2);

    private static final Map<Integer, Type> enumValues = unmodifiableMap(map(v -> T(v.value(), v), values()));
    private int value;

    Type(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }

    public static Map<Integer, Type> getValueMap() {
      return enumValues;
    }
  }

  @PartitionKey
  private UUID id;
  @Column(name = "namespace_id")
  private UUID namespaceID;
  @Column(name = "organization_id")
  private UUID organizationID;
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

  public UUID getOrganizationID() {
    return organizationID;
  }

  public SourceEntity setOrganizationID(UUID organizationID) {
    this.organizationID = organizationID;
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
