package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AclEntryTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeAclEntry() {
    AclEntry entry = AclEntry.builder()
            .setId(UUID.randomUUID())
            .setSubject(Subject.builder().setId(UUID.randomUUID()).setName("subject").build().toInfo())
            .setSource(Source.builder().setId(UUID.randomUUID()).setName("source").build().toInfo())
            .setTimestamp(1480520821000L)
            .build();

    JsonNode root = mapper.valueToTree(entry);
    assertEquals(entry.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("subject").isObject());
    assertTrue(root.get("source").isObject());
    assertEquals("2016-11-30T15:47:01Z", root.get("timestamp").textValue());
  }

}
