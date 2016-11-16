package no.mnemonic.act.platform.entity.cassandra;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ObjectEntityTest {

  @Test
  public void testCloneEntity() {
    ObjectEntity original = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");
    ObjectEntity clone = original.clone();

    assertNotSame(original, clone);
    assertObject(original, clone);
  }

  private void assertObject(ObjectEntity expected, ObjectEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
  }

}
