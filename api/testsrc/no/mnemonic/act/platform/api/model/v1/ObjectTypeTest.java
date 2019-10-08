package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectTypeTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeObjectType() {
    ObjectType type = createObjectType();
    JsonNode root = mapper.valueToTree(type);
    assertEquals(type.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("namespace").isObject());
    assertEquals(type.getName(), root.get("name").textValue());
    assertEquals(type.getValidator(), root.get("validator").textValue());
    assertEquals(type.getValidatorParameter(), root.get("validatorParameter").textValue());
  }

  @Test
  public void testEncodeObjectTypeInfo() {
    ObjectType.Info type = createObjectType().toInfo();
    JsonNode root = mapper.valueToTree(type);
    assertEquals(type.getId().toString(), root.get("id").textValue());
    assertEquals(type.getName(), root.get("name").textValue());
  }

  private ObjectType createObjectType() {
    return ObjectType.builder()
            .setId(UUID.randomUUID())
            .setNamespace(Namespace.builder().setId(UUID.randomUUID()).setName("namespace").build())
            .setName("objectType")
            .setValidator("validator")
            .setValidatorParameter("validatorParameter")
            .build();
  }

}
