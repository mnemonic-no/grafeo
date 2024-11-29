package no.mnemonic.services.grafeo.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AclEntrySEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "subject : {}," +
            "origin : {}," +
            "timestamp : '2016-11-30T15:47:01Z'" +
            "}", id);

    AclEntrySEB model = getMapper().readValue(json, AclEntrySEB.class);
    assertEquals(id, model.getId());
    assertNotNull(model.getSubject());
    assertNotNull(model.getOrigin());
    assertEquals(1480520821000L, model.getTimestamp());
  }

  @Test
  public void testDecodeWithUnknownProperty() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "unknown : 'Should be ignored'" +
            "}", id);

    AclEntrySEB model = getMapper().readValue(json, AclEntrySEB.class);
    assertEquals(id, model.getId());
  }

  @Test
  public void testEncode() {
    AclEntrySEB model = AclEntrySEB.builder()
            .setId(UUID.randomUUID())
            .setSubject(SubjectInfoSEB.builder().build())
            .setOrigin(OriginInfoSEB.builder().build())
            .setTimestamp(1480520821000L)
            .build();
    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("subject").isObject());
    assertTrue(root.get("origin").isObject());
    assertEquals("2016-11-30T15:47:01Z", root.get("timestamp").textValue());
  }
}
