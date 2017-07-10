package no.mnemonic.act.platform.auth.properties.internal;

import java.util.UUID;

/**
 * Provides helper methods to map between global and internal IDs.
 */
public class IdMapper {

  /**
   * Maps a numeric internal ID to a global UUID.
   *
   * @param id Internal ID, must be &gt;= 0.
   * @return Mapped global UUID
   */
  public static UUID toGlobalID(long id) {
    if (id < 0) throw new IllegalArgumentException("id must not be negative.");
    return new UUID(0, id);
  }

  /**
   * Maps a global UUID to a numeric internal ID.
   *
   * @param id Global ID, must not be null.
   * @return Mapped internal ID
   */
  public static long toInternalID(UUID id) {
    if (id == null) throw new IllegalArgumentException("id must not be null.");
    if (id.getMostSignificantBits() != 0) throw new IllegalArgumentException("most significant bits must not be set.");
    if (id.getLeastSignificantBits() < 0) throw new IllegalArgumentException("least significant bits must not be negative.");
    return id.getLeastSignificantBits();
  }

}
