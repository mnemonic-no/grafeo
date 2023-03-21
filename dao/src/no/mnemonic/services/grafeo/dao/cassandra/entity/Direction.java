package no.mnemonic.services.grafeo.dao.cassandra.entity;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

public enum Direction implements CassandraEnum<Direction> {
  // Direction.None had value of 0, but None is no longer in use and has been removed.
  FactIsSource(1), FactIsDestination(2), BiDirectional(3);

  private static final Map<Integer, Direction> enumValues = unmodifiableMap(map(v -> T(v.value(), v), values()));
  private int value;

  Direction(int value) {
    this.value = value;
  }

  @Override
  public int value() {
    return value;
  }

  public static Map<Integer, Direction> getValueMap() {
    return enumValues;
  }
}
