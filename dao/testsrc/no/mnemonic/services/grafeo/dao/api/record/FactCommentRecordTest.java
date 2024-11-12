package no.mnemonic.services.grafeo.dao.api.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FactCommentRecordTest {

  private final ObjectMapper MAPPER = JsonMapper.builder().build();
  private final ObjectReader READER = MAPPER.readerFor(FactCommentRecord.class);
  private final ObjectWriter WRITER = MAPPER.writerFor(FactCommentRecord.class);

  @Test
  public void testSerializeAndDeserialize() throws Exception {
    FactCommentRecord expected = new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setComment("comment")
            .setTimestamp(123456789);

    FactCommentRecord actual = READER.readValue(WRITER.writeValueAsBytes(expected), FactCommentRecord.class);
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getReplyToID(), actual.getReplyToID());
    assertEquals(expected.getOriginID(), actual.getOriginID());
    assertEquals(expected.getComment(), actual.getComment());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  @Test
  public void testIgnoreUnknownProperties() throws Exception {
    assertNotNull(READER.readValue("{ \"unknown\" : 42 }", FactCommentRecord.class));
  }
}
