package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class SubjectInfoSEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "name : 'name'" +
            "}", id);

    SubjectInfoSEB model = getMapper().readValue(json, SubjectInfoSEB.class);
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

    SubjectInfoSEB model = getMapper().readValue(json, SubjectInfoSEB.class);
    assertEquals(id, model.getId());
  }

  @Test
  public void testEncode() {
    SubjectInfoSEB model = SubjectInfoSEB.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertEquals(model.getName(), root.get("name").textValue());
  }
}
