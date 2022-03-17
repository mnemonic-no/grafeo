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
import static no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity.TABLE;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

@Entity(defaultKeyspace = KEY_SPACE)
@CqlName(TABLE)
public class ObjectTypeEntity implements CassandraEntity {

  public static final String TABLE = "object_type";

  public enum Flag implements CassandraEnum<Flag> {
    Deleted(0),
    TimeGlobalIndex(1);

    private static final Map<Integer, Flag> enumValues = unmodifiableMap(map(v -> T(v.value(), v), values()));
    private final int value;

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
  private String name;
  private String validator;
  @CqlName("validator_parameter")
  private String validatorParameter;
  private Set<Flag> flags;

  public UUID getId() {
    return id;
  }

  public ObjectTypeEntity setId(UUID id) {
    this.id = id;
    return this;
  }

  public UUID getNamespaceID() {
    return namespaceID;
  }

  public ObjectTypeEntity setNamespaceID(UUID namespaceID) {
    this.namespaceID = namespaceID;
    return this;
  }

  public String getName() {
    return name;
  }

  public ObjectTypeEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getValidator() {
    return validator;
  }

  public ObjectTypeEntity setValidator(String validator) {
    this.validator = validator;
    return this;
  }

  public String getValidatorParameter() {
    return validatorParameter;
  }

  public ObjectTypeEntity setValidatorParameter(String validatorParameter) {
    this.validatorParameter = validatorParameter;
    return this;
  }

  public Set<Flag> getFlags() {
    return flags;
  }

  public ObjectTypeEntity setFlags(Set<Flag> flags) {
    this.flags = flags;
    return this;
  }

  public ObjectTypeEntity addFlag(Flag flag) {
    this.flags = SetUtils.addToSet(this.flags, flag);
    return this;
  }

  public boolean isSet(Flag flag) {
    return SetUtils.set(flags).contains(flag);
  }

}
