package no.mnemonic.act.platform.dao.api.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ObjectRecordTest {

  private final ObjectMapper MAPPER = JsonMapper.builder().build();
  private final ObjectReader READER = MAPPER.readerFor(ObjectRecord.class);
  private final ObjectWriter WRITER = MAPPER.writerFor(ObjectRecord.class);

  @Test
  public void testSerializeAndDeserialize() throws Exception {
    ObjectRecord expected = new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");

    ObjectRecord actual = READER.readValue(WRITER.writeValueAsBytes(expected), ObjectRecord.class);
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
  }

  @Test
  public void testIgnoreUnknownProperties() throws Exception {
    assertNotNull(READER.readValue("{ \"unknown\" : 42 }", ObjectRecord.class));
  }
}
