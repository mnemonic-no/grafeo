package no.mnemonic.services.grafeo.service.providers;

import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class HazelcastJsonSerializerTest {

  private final HazelcastJsonSerializer<FactSEB> serializer = new HazelcastJsonSerializer<>(FactSEB.class, 46616374);

  @Test
  public void testSerializeNull() throws Exception {
    assertNotNull(serializer.write(null));
  }

  @Test
  public void testDeserializeNull() throws Exception {
    assertNull(serializer.read(null));
  }

  @Test
  public void testSerializeAndDeserializeNull() throws Exception {
    assertNull(serializer.read(serializer.write(null)));
  }

  @Test
  public void testSerializeAndDeserializeObject() throws Exception {
    FactSEB expected = FactSEB.builder()
            .setId(UUID.randomUUID())
            .build();

    FactSEB actual = serializer.read(serializer.write(expected));
    assertNotNull(actual);
    assertEquals(expected.getId(), actual.getId());
  }
}
