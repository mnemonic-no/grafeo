package no.mnemonic.services.grafeo.dao.api.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactRecordTest {

  private final ObjectMapper MAPPER = JsonMapper.builder().build();
  private final ObjectReader READER = MAPPER.readerFor(FactRecord.class);
  private final ObjectWriter WRITER = MAPPER.writerFor(FactRecord.class);

  @Test
  public void testSerializeAndDeserialize() throws Exception {
    FactRecord expected = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setLastSeenByID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setSourceObject(new ObjectRecord().setId(UUID.randomUUID()))
            .setDestinationObject(new ObjectRecord().setId(UUID.randomUUID()))
            .setBidirectionalBinding(true)
            .addFlag(FactRecord.Flag.TimeGlobalIndex)
            .addAclEntry(new FactAclEntryRecord().setId(UUID.randomUUID()))
            .addComment(new FactCommentRecord().setId(UUID.randomUUID()));

    FactRecord actual = READER.readValue(WRITER.writeValueAsBytes(expected), FactRecord.class);
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expected.getInReferenceToID(), actual.getInReferenceToID());
    assertEquals(expected.getOrganizationID(), actual.getOrganizationID());
    assertEquals(expected.getOriginID(), actual.getOriginID());
    assertEquals(expected.getAddedByID(), actual.getAddedByID());
    assertEquals(expected.getLastSeenByID(), actual.getLastSeenByID());
    assertEquals(expected.getAccessMode(), actual.getAccessMode());
    assertEquals(expected.getConfidence(), actual.getConfidence(), 0.0);
    assertEquals(expected.getTrust(), actual.getTrust(), 0.0);
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getLastSeenTimestamp(), actual.getLastSeenTimestamp());
    assertNotNull(actual.getSourceObject());
    assertNotNull(actual.getDestinationObject());
    assertTrue(actual.isBidirectionalBinding());
    assertEquals(expected.getFlags(), actual.getFlags());
    assertEquals(1, actual.getAcl().size());
    assertEquals(1, actual.getComments().size());
  }

  @Test
  public void testIgnoreUnknownProperties() throws Exception {
    assertNotNull(READER.readValue("{ \"unknown\" : 42 }", FactRecord.class));
  }
}
