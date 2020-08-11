package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class OrganizationInfoSEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "name : 'name'" +
            "}", id);

    OrganizationInfoSEB model = getMapper().readValue(json, OrganizationInfoSEB.class);
    assertEquals(id, model.getId());
    assertEquals("name", model.getName());
  }

  @Test
  public void testEncode() {
    OrganizationInfoSEB model = OrganizationInfoSEB.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertEquals(model.getName(), root.get("name").textValue());
  }
}
