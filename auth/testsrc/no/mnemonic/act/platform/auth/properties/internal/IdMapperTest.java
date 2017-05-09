package no.mnemonic.act.platform.auth.properties.internal;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class IdMapperTest {

  @Test
  public void testToGlobalId() {
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), IdMapper.toGlobalID(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToGlobalIdNegativeIdThrowsException() {
    IdMapper.toGlobalID(-1);
  }

  @Test
  public void testToInternalId() {
    assertEquals(1, IdMapper.toInternalID(UUID.fromString("00000000-0000-0000-0000-000000000001")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToInternalIdNullIdThrowsException() {
    IdMapper.toInternalID(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToInternalIdMostSignificantBitsSetThrowsException() {
    IdMapper.toInternalID(new UUID(1, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToInternalIdLeastSignificantBitsNegativeThrowsException() {
    IdMapper.toInternalID(new UUID(0, -1));
  }

}
