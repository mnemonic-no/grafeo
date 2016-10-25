package no.mnemonic.act.platform.entity.cassandra;

import java.util.HashMap;
import java.util.Map;

public enum Direction implements CassandraEnum<Direction> {
  None(0), FactIsSource(1), FactIsDestination(2), BiDirectional(3);

  private int value;

  Direction(int value) {
    this.value = value;
  }

  @Override
  public int value() {
    return value;
  }

  public static Map<Integer, Direction> getValueMap() {
    // After we have moved the *Util classes we can be a little bit smarter here
    // and not generate the map on every access.
    Map<Integer, Direction> enumValues = new HashMap<>();
    for (Direction direction : values()) {
      enumValues.put(direction.value(), direction);
    }

    return enumValues;
  }
}
