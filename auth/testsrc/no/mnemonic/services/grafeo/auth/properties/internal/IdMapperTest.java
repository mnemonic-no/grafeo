package no.mnemonic.services.grafeo.auth.properties.internal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdMapperTest {

  @Test
  public void testToGlobalId() {
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), IdMapper.toGlobalID(1));
  }

  @Test
  public void testToGlobalIdNegativeIdThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> IdMapper.toGlobalID(-1));
  }

  @Test
  public void testToInternalId() {
    assertEquals(1, IdMapper.toInternalID(UUID.fromString("00000000-0000-0000-0000-000000000001")));
  }

  @Test
  public void testToInternalIdNullIdThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> IdMapper.toInternalID(null));
  }

  @Test
  public void testToInternalIdMostSignificantBitsSetThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> IdMapper.toInternalID(new UUID(1, 1)));
  }

  @Test
  public void testToInternalIdLeastSignificantBitsNegativeThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> IdMapper.toInternalID(new UUID(0, -1)));
  }

}
