package no.mnemonic.act.platform.entity.cassandra;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

public enum Direction implements CassandraEnum<Direction> {
  None(0), FactIsSource(1), FactIsDestination(2), BiDirectional(3);

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
