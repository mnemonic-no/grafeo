package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;
import static no.mnemonic.act.platform.dao.cassandra.entity.CassandraEntity.KEY_SPACE;
import static no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity.TABLE;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class OriginEntity implements CassandraEntity {

  public static final String TABLE = "origin";

  public enum Type implements CassandraEnum<Type> {
    Group(0), User(1);

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

  public enum Flag implements CassandraEnum<Flag> {
    Deleted(0);

    private static final Map<Integer, Flag> enumValues = unmodifiableMap(map(v -> T(v.value(), v), values()));
    private int value;

    Flag(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }

    public static Map<Integer, Flag> getValueMap() {
      return enumValues;
    }
  }

  @PartitionKey
  private UUID id;
  @CqlName("namespace_id")
  private UUID namespaceID;
  @CqlName("organization_id")
  private UUID organizationID;
  private String name;
  private String description;
  private float trust;
  private Type type;
  private Set<Flag> flags;

  public UUID getId() {
    return id;
  }

  public OriginEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getNamespaceID() {
    return namespaceID;
  }

  public OriginEntity setNamespaceID(UUID namespaceID) {
    this.namespaceID = namespaceID;
    return this;
  }

  public UUID getOrganizationID() {
    return organizationID;
  }

  public OriginEntity setOrganizationID(UUID organizationID) {
    this.organizationID = organizationID;
    return this;
  }

  public String getName() {
    return name;
  }

  public OriginEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public OriginEntity setDescription(String description) {
    this.description = description;
    return this;
  }

  public float getTrust() {
    return trust;
  }

  public OriginEntity setTrust(float trust) {
    this.trust = trust;
    return this;
  }

  public Type getType() {
    return type;
  }

  public OriginEntity setType(Type type) {
    this.type = type;
    return this;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public OriginEntity setFlags(Set<Flag> flags) {
    this.flags = flags;
    return this;
  }

  public OriginEntity addFlag(Flag flag) {
    this.flags = SetUtils.addToSet(this.flags, flag);
    return this;
  }

}
