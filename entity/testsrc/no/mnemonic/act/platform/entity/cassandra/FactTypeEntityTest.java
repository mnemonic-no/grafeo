package no.mnemonic.act.platform.entity.cassandra;

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
            createFactObjectBindingDefinition(Direction.None),
            createFactObjectBindingDefinition(Direction.BiDirectional)
    );
    FactTypeEntity entity = new FactTypeEntity().setRelevantObjectBindings(bindings);

    assertFactObjectBindingDefinitions(bindings, entity.getRelevantObjectBindingsStored());
  }

  @Test
  public void setRelevantObjectBindingsFromString() throws IOException {
    String bindings = "[{\"objectTypeID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\",\"direction\":0},{\"objectTypeID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\",\"direction\":3}]";
    FactTypeEntity entity = new FactTypeEntity().setRelevantObjectBindingsStored(bindings);

    assertFactObjectBindingDefinitions(entity.getRelevantObjectBindings(), bindings);
  }

  private FactTypeEntity.FactObjectBindingDefinition createFactObjectBindingDefinition(Direction direction) {
    return new FactTypeEntity.FactObjectBindingDefinition()
            .setObjectTypeID(UUID.randomUUID())
            .setDirection(direction);
  }

  private void assertFactObjectBindingDefinitions(List<FactTypeEntity.FactObjectBindingDefinition> bindings, String json) throws IOException {
    JsonNode node = reader.readTree(json);

    assertEquals(bindings.size(), node.size());
    for (int i = 0; i < node.size(); i++) {
      assertEquals(bindings.get(i).getDirectionValue(), node.get(i).get("direction").asInt());
      assertEquals(bindings.get(i).getObjectTypeID().toString(), node.get(i).get("objectTypeID").asText());
    }
  }

}
