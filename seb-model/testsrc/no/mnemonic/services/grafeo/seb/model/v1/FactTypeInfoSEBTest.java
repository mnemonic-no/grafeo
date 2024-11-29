package no.mnemonic.services.grafeo.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactTypeInfoSEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "name : 'name'" +
            "}", id);

    FactTypeInfoSEB model = getMapper().readValue(json, FactTypeInfoSEB.class);
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

    FactTypeInfoSEB model = getMapper().readValue(json, FactTypeInfoSEB.class);
    assertEquals(id, model.getId());
  }

  @Test
  public void testEncode() {
    FactTypeInfoSEB model = FactTypeInfoSEB.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertEquals(model.getName(), root.get("name").textValue());
  }
}
