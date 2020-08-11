package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactInfoSEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "type : {}," +
            "value : 'value'" +
            "}", id);

    FactInfoSEB model = getMapper().readValue(json, FactInfoSEB.class);
    assertEquals(id, model.getId());
    assertNotNull(model.getType());
    assertEquals("value", model.getValue());
  }

  @Test
  public void testEncode() {
    FactInfoSEB model = FactInfoSEB.builder()
            .setId(UUID.randomUUID())
            .setType(FactTypeInfoSEB.builder().build())
            .setValue("value")
            .build();
    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("type").isObject());
    assertEquals(model.getValue(), root.get("value").textValue());
  }
}
