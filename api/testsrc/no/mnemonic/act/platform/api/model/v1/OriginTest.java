package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OriginTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeOrigin() {
    Origin origin = createOrigin();
    JsonNode root = mapper.valueToTree(origin);
    assertEquals(origin.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("namespace").isObject());
    assertTrue(root.get("organization").isObject());
    assertEquals(origin.getName(), root.get("name").textValue());
    assertEquals(origin.getDescription(), root.get("description").textValue());
    assertEquals(origin.getTrust(), root.get("trust").floatValue(), 0.0);
    assertEquals(origin.getType().toString(), root.get("type").textValue());
    assertEquals(set(origin.getFlags(), Enum::name), set(root.get("flags").iterator(), JsonNode::textValue));
  }

  @Test
  public void testEncodeOriginInfo() {
    Origin.Info origin = createOrigin().toInfo();
    JsonNode root = mapper.valueToTree(origin);
    assertEquals(origin.getId().toString(), root.get("id").textValue());
    assertEquals(origin.getName(), root.get("name").textValue());
  }

  private Origin createOrigin() {
    return Origin.builder()
            .setId(UUID.randomUUID())
            .setNamespace(Namespace.builder().setId(UUID.randomUUID()).setName("namespace").build())
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).setName("organization").build().toInfo())
            .setName("name")
            .setDescription("description")
            .setTrust(0.1f)
            .setType(Origin.Type.User)
            .addFlag(Origin.Flag.Deleted)
            .build();
  }

}
