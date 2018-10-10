package no.mnemonic.act.platform.dao.cassandra.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class FactTypeEntityTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final ObjectReader reader = mapper.reader();

  @Test
  public void setRelevantObjectBindingsFromObjects() throws IOException {
    Set<FactTypeEntity.FactObjectBindingDefinition> bindings = SetUtils.set(
            createFactObjectBindingDefinition(false),
            createFactObjectBindingDefinition(true)
    );
    FactTypeEntity entity = new FactTypeEntity().setRelevantObjectBindings(bindings);

    assertFactObjectBindingDefinitions(bindings, entity.getRelevantObjectBindingsStored());
  }

  @Test
  public void setRelevantObjectBindingsFromString() throws IOException {
    String bindings = "[{\"sourceObjectTypeID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\",\"destinationObjectTypeID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\",\"bidirectionalBinding\":false}," +
            "{\"sourceObjectTypeID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\",\"destinationObjectTypeID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\",\"bidirectionalBinding\":true}]";
    FactTypeEntity entity = new FactTypeEntity().setRelevantObjectBindingsStored(bindings);

    assertFactObjectBindingDefinitions(entity.getRelevantObjectBindings(), bindings);
  }

  private FactTypeEntity.FactObjectBindingDefinition createFactObjectBindingDefinition(boolean bidirectional) {
    return new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(UUID.randomUUID())
            .setDestinationObjectTypeID(UUID.randomUUID())
            .setBidirectionalBinding(bidirectional);
  }

  private void assertFactObjectBindingDefinitions(Set<FactTypeEntity.FactObjectBindingDefinition> bindings, String json) throws IOException {
    JsonNode node = reader.readTree(json);

    assertEquals(bindings.size(), node.size());
    for (int i = 0; i < node.size(); i++) {
      UUID sourceObjectTypeID = UUID.fromString(node.get(i).get("sourceObjectTypeID").asText());
      FactTypeEntity.FactObjectBindingDefinition expected = getBindingForSourceObjectTypeID(bindings, sourceObjectTypeID);
      assertEquals(expected.getSourceObjectTypeID(), sourceObjectTypeID);
      assertEquals(expected.getDestinationObjectTypeID().toString(), node.get(i).get("destinationObjectTypeID").asText());
      assertEquals(expected.isBidirectionalBinding(), node.get(i).get("bidirectionalBinding").asBoolean());
    }
  }

  private FactTypeEntity.FactObjectBindingDefinition getBindingForSourceObjectTypeID(
          Set<FactTypeEntity.FactObjectBindingDefinition> bindings, UUID sourceObjectTypeID) {
    return bindings.stream()
            .filter(b -> Objects.equals(b.getSourceObjectTypeID(), sourceObjectTypeID))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
  }

}
