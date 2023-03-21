package no.mnemonic.services.grafeo.seb.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactSEBTest extends AbstractSEBTest {

  @Test
  public void testDecode() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "type : {}," +
            "value : 'value'," +
            "inReferenceTo : {}," +
            "organization : {}," +
            "origin : {}," +
            "addedBy : {}," +
            "lastSeenBy : {}," +
            "accessMode : 'RoleBased'," +
            "confidence : 0.1," +
            "trust : 0.2," +
            "timestamp : '2016-11-30T15:47:01Z'," +
            "lastSeenTimestamp : '2016-11-30T15:47:02Z'," +
            "sourceObject : {}," +
            "destinationObject : {}," +
            "bidirectionalBinding : true," +
            "flags : ['RetractedHint']," +
            "acl : []" +
            "}", id);

    FactSEB model = getMapper().readValue(json, FactSEB.class);
    assertEquals(id, model.getId());
    assertNotNull(model.getType());
    assertEquals("value", model.getValue());
    assertNotNull(model.getInReferenceTo());
    assertNotNull(model.getOrganization());
    assertNotNull(model.getOrigin());
    assertNotNull(model.getAddedBy());
    assertNotNull(model.getLastSeenBy());
    assertEquals(FactSEB.AccessMode.RoleBased, model.getAccessMode());
    assertEquals(0.1f, model.getConfidence(), 0.0);
    assertEquals(0.2f, model.getTrust(), 0.0);
    assertEquals(1480520821000L, model.getTimestamp());
    assertEquals(1480520822000L, model.getLastSeenTimestamp());
    assertNotNull(model.getSourceObject());
    assertNotNull(model.getDestinationObject());
    assertTrue(model.isBidirectionalBinding());
    assertEquals(SetUtils.set(FactSEB.Flag.RetractedHint), model.getFlags());
    assertNotNull(model.getAcl());
  }

  @Test
  public void testDecodeWithUnknownProperty() throws Exception {
    UUID id = UUID.randomUUID();
    String json = String.format("{" +
            "id : '%s'," +
            "unknown : 'Should be ignored'" +
            "}", id);

    FactSEB model = getMapper().readValue(json, FactSEB.class);
    assertEquals(id, model.getId());
  }

  @Test
  public void testEncode() {
    FactSEB model = FactSEB.builder()
            .setId(UUID.randomUUID())
            .setType(FactTypeInfoSEB.builder().build())
            .setValue("value")
            .setInReferenceTo(FactInfoSEB.builder().build())
            .setOrganization(OrganizationInfoSEB.builder().build())
            .setOrigin(OriginInfoSEB.builder().build())
            .setAddedBy(SubjectInfoSEB.builder().build())
            .setLastSeenBy(SubjectInfoSEB.builder().build())
            .setAccessMode(FactSEB.AccessMode.RoleBased)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(1480520821000L)
            .setLastSeenTimestamp(1480520822000L)
            .setSourceObject(ObjectInfoSEB.builder().build())
            .setDestinationObject(ObjectInfoSEB.builder().build())
            .setBidirectionalBinding(true)
            .addFlag(FactSEB.Flag.RetractedHint)
            .addAclEntry(AclEntrySEB.builder().build())
            .build();

    JsonNode root = getMapper().valueToTree(model);
    assertEquals(model.getId().toString(), root.get("id").textValue());
    assertTrue(root.get("type").isObject());
    assertEquals(model.getValue(), root.get("value").textValue());
    assertTrue(root.get("inReferenceTo").isObject());
    assertTrue(root.get("organization").isObject());
    assertTrue(root.get("origin").isObject());
    assertTrue(root.get("addedBy").isObject());
    assertTrue(root.get("lastSeenBy").isObject());
    assertEquals(model.getAccessMode().name(), root.get("accessMode").textValue());
    assertEquals(model.getConfidence(), root.get("confidence").floatValue(), 0.0);
    assertEquals(model.getTrust(), root.get("trust").floatValue(), 0.0);
    assertEquals("2016-11-30T15:47:01Z", root.get("timestamp").textValue());
    assertEquals("2016-11-30T15:47:02Z", root.get("lastSeenTimestamp").textValue());
    assertTrue(root.get("sourceObject").isObject());
    assertTrue(root.get("destinationObject").isObject());
    assertTrue(root.get("bidirectionalBinding").booleanValue());
    assertEquals(model.getFlags(), SetUtils.set(root.get("flags").iterator(), node -> FactSEB.Flag.valueOf(node.textValue())));
    assertTrue(root.get("acl").isArray());
  }
}
