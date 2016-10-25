package no.mnemonic.act.platform.entity.cassandra;

import java.util.HashMap;
import java.util.Map;

public enum AccessMode implements CassandraEnum<AccessMode> {
  Public(0), RoleBased(1), Explicit(2);

  private int value;

  AccessMode(int value) {
    this.value = value;
  }

  @Override
  public int value() {
    return value;
  }

  public static Map<Integer, AccessMode> getValueMap() {
    // After we have moved the *Util classes we can be a little bit smarter here
    // and not generate the map on every access.
    Map<Integer, AccessMode> enumValues = new HashMap<>();
    for (AccessMode type : values()) {
      enumValues.put(type.value(), type);
    }

    return enumValues;
  }
}