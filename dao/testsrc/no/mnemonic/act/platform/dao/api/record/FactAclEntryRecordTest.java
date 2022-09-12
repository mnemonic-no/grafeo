package no.mnemonic.act.platform.dao.api.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FactAclEntryRecordTest {

  private final ObjectMapper MAPPER = JsonMapper.builder().build();
  private final ObjectReader READER = MAPPER.readerFor(FactAclEntryRecord.class);
  private final ObjectWriter WRITER = MAPPER.writerFor(FactAclEntryRecord.class);

  @Test
  public void testSerializeAndDeserialize() throws Exception {
    FactAclEntryRecord expected = new FactAclEntryRecord()
            .setId(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);

    FactAclEntryRecord actual = READER.readValue(WRITER.writeValueAsBytes(expected), FactAclEntryRecord.class);
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getSubjectID(), actual.getSubjectID());
    assertEquals(expected.getOriginID(), actual.getOriginID());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  @Test
  public void testIgnoreUnknownProperties() throws Exception {
    assertNotNull(READER.readValue("{ \"unknown\" : 42 }", FactAclEntryRecord.class));
  }
}
