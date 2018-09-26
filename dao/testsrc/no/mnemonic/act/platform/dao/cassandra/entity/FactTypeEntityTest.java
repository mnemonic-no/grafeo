package no.mnemonic.act.platform.dao.cassandra.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class FactTypeEntityTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final ObjectReader reader = mapper.reader();

  @Test
  public void setRelevantObjectBindingsFromObjects() throws IOException {
    List<FactTypeEntity.FactObjectBindingDefinition> bindings = Arrays.asList(
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

  private void assertFactObjectBindingDefinitions(List<FactTypeEntity.FactObjectBindingDefinition> bindings, String json) throws IOException {
    JsonNode node = reader.readTree(json);

    assertEquals(bindings.size(), node.size());
    for (int i = 0; i < node.size(); i++) {
      assertEquals(bindings.get(i).getSourceObjectTypeID().toString(), node.get(i).get("sourceObjectTypeID").asText());
      assertEquals(bindings.get(i).getDestinationObjectTypeID().toString(), node.get(i).get("destinationObjectTypeID").asText());
      assertEquals(bindings.get(i).isBidirectionalBinding(), node.get(i).get("bidirectionalBinding").asBoolean());
    }
  }

}
