package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class ObjectInfoSEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "type : {}," +
            "value : 'value'" +
            "}", id);

    ObjectInfoSEB model = getMapper().readValue(json, ObjectInfoSEB.class);
    assertEquals(id, model.getId());
    assertNotNull(model.getType());
    assertEquals("value", model.getValue());
  }

  @Test
  public void testEncode() {
    ObjectInfoSEB model = ObjectInfoSEB.builder()
            .setId(UUID.randomUUID())
            .setType(ObjectTypeInfoSEB.builder().build())
            .setValue("value")
            .build();
    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("type").isObject());
    assertEquals(model.getValue(), root.get("value").textValue());
  }
}
