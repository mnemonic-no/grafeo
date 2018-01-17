package no.mnemonic.act.platform.dao.cassandra.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class FactEntityTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final ObjectReader reader = mapper.reader();

  @Test
  public void setBindingsFromObjects() throws IOException {
    List<FactEntity.FactObjectBinding> bindings = Arrays.asList(
            createFactObjectBinding(Direction.None),
            createFactObjectBinding(Direction.BiDirectional)
    );
    FactEntity entity = new FactEntity().setBindings(bindings);

    assertFactObjectBindings(bindings, entity.getBindingsStored());
  }

  @Test
  public void setBindingsFromString() throws IOException {
    String bindings = "[{\"objectID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\",\"direction\":0},{\"objectID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\",\"direction\":3}]";
    FactEntity entity = new FactEntity().setBindingsStored(bindings);

    assertFactObjectBindings(entity.getBindings(), bindings);
  }

  @Test
  public void testCloneEntity() throws IOException {
    FactEntity original = createFact();
    FactEntity clone = original.clone();

    assertNotSame(original, clone);
    assertFact(original, clone);
  }

  private FactEntity createFact() throws IOException {
    return new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setAccessMode(AccessMode.Public)
            .setConfidenceLevel(0)
            .setTimestamp(1)
            .setLastSeenTimestamp(2)
            .setBindings(Collections.singletonList(createFactObjectBinding(Direction.None)));
  }

  private FactEntity.FactObjectBinding createFactObjectBinding(Direction direction) {
    return new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(direction);
  }

  private void assertFact(FactEntity expected, FactEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expected.getInReferenceToID(), actual.getInReferenceToID());
    assertEquals(expected.getOrganizationID(), actual.getOrganizationID());
    assertEquals(expected.getSourceID(), actual.getSourceID());
    assertEquals(expected.getAccessMode(), actual.getAccessMode());
    assertEquals(expected.getConfidenceLevel(), actual.getConfidenceLevel());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getLastSeenTimestamp(), actual.getLastSeenTimestamp());
    assertEquals(expected.getBindingsStored(), actual.getBindingsStored());
  }

  private void assertFactObjectBindings(List<FactEntity.FactObjectBinding> bindings, String json) throws IOException {
    JsonNode node = reader.readTree(json);

    assertEquals(bindings.size(), node.size());
    for (int i = 0; i < node.size(); i++) {
      assertEquals(bindings.get(i).getDirectionValue(), node.get(i).get("direction").asInt());
      assertEquals(bindings.get(i).getObjectID().toString(), node.get(i).get("objectID").asText());
    }
  }

}
