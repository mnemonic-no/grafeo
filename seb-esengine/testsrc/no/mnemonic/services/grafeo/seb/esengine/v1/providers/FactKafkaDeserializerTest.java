package no.mnemonic.services.grafeo.seb.esengine.v1.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactKafkaDeserializerTest {

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();
  private static final ObjectWriter WRITER = MAPPER.writerFor(FactSEB.class);

  private final FactKafkaDeserializer deserializer = new FactKafkaDeserializer();

  @Test
  public void testDeserializeNull() throws Exception {
    assertNull(deserializer.deserialize("test", null));
    assertNull(deserializer.deserialize("test", WRITER.writeValueAsBytes(null)));
  }

  @Test
  public void testDeserializeObject() throws Exception {
    FactSEB expected = FactSEB.builder()
            .setId(UUID.randomUUID())
            .build();

    FactSEB actual = deserializer.deserialize("test", WRITER.writeValueAsBytes(expected));
    assertNotNull(actual);
    assertEquals(expected.getId(), actual.getId());
  }
}
