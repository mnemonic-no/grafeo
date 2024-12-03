package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

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
            .setLastAddedTimestamp(1480520821L)
            .setLastSeenTimestamp(1480520822L)
            .build();
  }

}
