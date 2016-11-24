package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeObject() {
    Object object = createObject();
    JsonNode root = mapper.valueToTree(object);
    assertEquals(object.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("type").isObject());
    assertEquals(object.getValue(), root.get("value").textValue());
    assertTrue(root.get("statistics").isArray());
    assertTrue(root.get("statistics").get(0).isObject());
  }

  @Test
  public void testEncodeObjectInfo() {
    Object.Info object = createObject().toInfo();
    JsonNode root = mapper.valueToTree(object);
    assertEquals(object.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("type").isObject());
    assertEquals(object.getValue(), root.get("value").textValue());
  }

  private Object createObject() {
    return Object.builder()
            .setId(UUID.randomUUID())
            .setType(ObjectType.builder().setId(UUID.randomUUID()).setName("objectType").build().toInfo())
            .setValue("value")
            .addStatistic(createObjectFactsStatistic())
            .build();
  }

  private ObjectFactsStatistic createObjectFactsStatistic() {
    return ObjectFactsStatistic.builder()
            .setType(FactType.builder().setId(UUID.randomUUID()).setName("factType").build().toInfo())
            .setCount(42)
            .setLastAddedTimestamp("lastAdded")
            .setLastSeenTimestamp("lastSeen")
            .build();
  }

}
