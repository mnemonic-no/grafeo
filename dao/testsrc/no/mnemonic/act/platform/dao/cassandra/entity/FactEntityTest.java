package no.mnemonic.act.platform.dao.cassandra.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class FactEntityTest {
  private static final ObjectMapper mapper = JsonMapper.builder().build();
  private static final ObjectReader reader = mapper.reader();

  @Test
  public void setBindingsFromObjects() throws IOException {
    List<FactEntity.FactObjectBinding> bindings = Arrays.asList(
            createFactObjectBinding(Direction.FactIsSource),
            createFactObjectBinding(Direction.BiDirectional)
    );
    FactEntity entity = new FactEntity().setBindings(bindings);

    assertFactObjectBindings(bindings, entity.getBindingsStored());
  }

  @Test
  public void addBindingFromObject() throws IOException {
    FactEntity entity = new FactEntity()
            .addBinding(createFactObjectBinding(Direction.FactIsSource))
            .addBinding(createFactObjectBinding(Direction.BiDirectional));

    assertFactObjectBindings(entity.getBindings(), entity.getBindingsStored());
  }

  @Test
  public void setBindingsFromString() throws IOException {
    String bindings = "[{\"objectID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\",\"direction\":1},{\"objectID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\",\"direction\":3}]";
    FactEntity entity = new FactEntity().setBindingsStored(bindings);

    assertFactObjectBindings(entity.getBindings(), bindings);
  }

  private FactEntity.FactObjectBinding createFactObjectBinding(Direction direction) {
    return new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(direction);
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
