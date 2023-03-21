package no.mnemonic.services.grafeo.seb.producer.v1.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactKafkaSerializerTest {

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();
  private static final ObjectReader READER = MAPPER.readerFor(FactSEB.class);

  private final FactKafkaSerializer serializer = new FactKafkaSerializer();

  @Test
  public void testSerializeNull() throws Exception {
    assertNull(READER.<FactSEB>readValue(serializer.serialize("test", null)));
  }

  @Test
  public void testSerializeObject() throws Exception {
    FactSEB expected = FactSEB.builder()
            .setId(UUID.randomUUID())
            .build();

    FactSEB actual = READER.readValue(serializer.serialize("test", expected));
    assertNotNull(actual);
    assertEquals(expected.getId(), actual.getId());
  }
}
