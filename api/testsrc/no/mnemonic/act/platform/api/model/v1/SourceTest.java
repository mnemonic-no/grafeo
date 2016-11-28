package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeSource() {
    Source source = createSource();
    JsonNode root = mapper.valueToTree(source);
    assertEquals(source.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("namespace").isObject());
    assertTrue(root.get("organization").isObject());
    assertEquals(source.getName(), root.get("name").textValue());
    assertEquals(source.getType().toString(), root.get("type").textValue());
  }

  @Test
  public void testEncodeSourceInfo() {
    Source.Info source = createSource().toInfo();
    JsonNode root = mapper.valueToTree(source);
    assertEquals(source.getId().toString(), root.get("id").textValue());
    assertEquals(source.getName(), root.get("name").textValue());
  }

  private Source createSource() {
    return Source.builder()
            .setId(UUID.randomUUID())
            .setNamespace(Namespace.builder().setId(UUID.randomUUID()).setName("namespace").build())
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).setName("organization").build().toInfo())
            .setName("source")
            .setType(Source.Type.User)
            .build();
  }

}
