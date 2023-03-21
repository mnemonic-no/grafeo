package no.mnemonic.services.grafeo.dao.cassandra.entity;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

public enum AccessMode implements CassandraEnum<AccessMode> {
  Public(0), RoleBased(1), Explicit(2);

  private static final Map<Integer, AccessMode> enumValues = unmodifiableMap(map(v -> T(v.value(), v), values()));
  private int value;

  AccessMode(int value) {
    this.value = value;
  }

  @Override
  public int value() {
    return value;
  }

  public static Map<Integer, AccessMode> getValueMap() {
    return enumValues;
  }
}