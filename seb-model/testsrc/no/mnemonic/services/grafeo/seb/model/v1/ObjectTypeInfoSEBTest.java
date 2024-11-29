package no.mnemonic.services.grafeo.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectTypeInfoSEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "name : 'name'" +
            "}", id);

    ObjectTypeInfoSEB model = getMapper().readValue(json, ObjectTypeInfoSEB.class);
    assertEquals(id, model.getId());
    assertEquals("name", model.getName());
  }

  @Test
  public void testDecodeWithUnknownProperty() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "unknown : 'Should be ignored'" +
            "}", id);

    ObjectTypeInfoSEB model = getMapper().readValue(json, ObjectTypeInfoSEB.class);
    assertEquals(id, model.getId());
  }

  @Test
  public void testEncode() {
    ObjectTypeInfoSEB model = ObjectTypeInfoSEB.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertEquals(model.getName(), root.get("name").textValue());
  }
}
