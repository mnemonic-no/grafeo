package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FactTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeFact() {
    Fact fact = createFact();
    JsonNode root = mapper.valueToTree(fact);
    assertEquals(fact.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("type").isObject());
    assertEquals(fact.getValue(), root.get("value").textValue());
    assertTrue(root.get("inReferenceTo").isObject());
    assertTrue(root.get("organization").isObject());
    assertTrue(root.get("source").isObject());
    assertEquals(fact.getAccessMode().toString(), root.get("accessMode").textValue());
    assertEquals(fact.getTimestamp(), root.get("timestamp").textValue());
    assertEquals(fact.getLastSeenTimestamp(), root.get("lastSeenTimestamp").textValue());
    assertTrue(root.get("objects").isArray());
    assertTrue(root.get("objects").get(0).get("object").isObject());
    assertEquals(fact.getObjects().get(0).getDirection().toString(), root.get("objects").get(0).get("direction").textValue());
  }

  @Test
  public void testEncodeFactInfo() {
    Fact.Info fact = createFactInfo();
    JsonNode root = mapper.valueToTree(fact);
    assertEquals(fact.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("type").isObject());
    assertEquals(fact.getValue(), root.get("value").textValue());
  }

  private Fact createFact() {
    return Fact.builder()
            .setId(UUID.randomUUID())
            .setType(createFactTypeInfo())
            .setValue("value")
            .setInReferenceTo(createFactInfo())
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).setName("organization").build().toInfo())
            .setSource(Source.builder().setId(UUID.randomUUID()).setName("source").build().toInfo())
            .setAccessMode(AccessMode.Explicit)
            .setTimestamp("timestamp")
            .setLastSeenTimestamp("lastSeen")
            .addObject(new Fact.FactObjectBinding(createObjectInfo(), Direction.BiDirectional))
            .build();
  }

  private Fact.Info createFactInfo() {
    return Fact.builder()
            .setId(UUID.randomUUID())
            .setType(createFactTypeInfo())
            .setValue("value")
            .build()
            .toInfo();
  }

  private FactType.Info createFactTypeInfo() {
    return FactType.builder()
            .setId(UUID.randomUUID())
            .setName("factType")
            .build()
            .toInfo();
  }

  private Object.Info createObjectInfo() {
    return Object.builder()
            .setId(UUID.randomUUID())
            .setType(ObjectType.builder().setId(UUID.randomUUID()).setName("objectType").build().toInfo())
            .setValue("value")
            .build()
            .toInfo();
  }

}
