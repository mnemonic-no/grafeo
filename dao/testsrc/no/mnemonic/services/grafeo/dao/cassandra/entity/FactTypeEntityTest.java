package no.mnemonic.services.grafeo.dao.cassandra.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactTypeEntityTest {
  private static final ObjectMapper mapper = JsonMapper.builder().build();
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
  public void addRelevantObjectBindingFromObject() throws IOException {
    FactTypeEntity entity = new FactTypeEntity()
            .addRelevantObjectBinding(createFactObjectBindingDefinition(false))
            .addRelevantObjectBinding(createFactObjectBindingDefinition(true));

    assertFactObjectBindingDefinitions(entity.getRelevantObjectBindings(), entity.getRelevantObjectBindingsStored());
  }

  @Test
  public void setRelevantObjectBindingsFromString() throws IOException {
    String bindings = "[{\"sourceObjectTypeID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\",\"destinationObjectTypeID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\",\"bidirectionalBinding\":false}," +
            "{\"sourceObjectTypeID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\",\"destinationObjectTypeID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\",\"bidirectionalBinding\":true}]";
    FactTypeEntity entity = new FactTypeEntity().setRelevantObjectBindingsStored(bindings);

    assertFactObjectBindingDefinitions(entity.getRelevantObjectBindings(), bindings);
  }

  @Test
  public void setRelevantFactBindingsFromObjects() throws IOException {
    Set<FactTypeEntity.MetaFactBindingDefinition> bindings = SetUtils.set(
            new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID()),
            new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID())
    );
    FactTypeEntity entity = new FactTypeEntity().setRelevantFactBindings(bindings);

    assertMetaFactBindingDefinitions(bindings, entity.getRelevantFactBindingsStored());
  }

  @Test
  public void setRelevantFactBindingsFromString() throws IOException {
    String bindings = "[{\"factTypeID\":\"ad35e1ec-e42f-4509-bbc8-6516a90b66e8\"}, {\"factTypeID\":\"95959968-f2fb-4913-9c0b-fc1b9144b60f\"}]";
    FactTypeEntity entity = new FactTypeEntity().setRelevantFactBindingsStored(bindings);

    assertMetaFactBindingDefinitions(entity.getRelevantFactBindings(), bindings);
  }

  @Test
  public void addRelevantFactBindingFromObject() throws IOException {
    FactTypeEntity entity = new FactTypeEntity()
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID()))
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID()));

    assertMetaFactBindingDefinitions(entity.getRelevantFactBindings(), entity.getRelevantFactBindingsStored());
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

  private void assertMetaFactBindingDefinitions(Set<FactTypeEntity.MetaFactBindingDefinition> bindings, String json) throws IOException {
    Set<UUID> expected = SetUtils.set(bindings, FactTypeEntity.MetaFactBindingDefinition::getFactTypeID);
    Set<UUID> actual = SetUtils.set(reader.readTree(json).iterator(), n -> UUID.fromString(n.get("factTypeID").asText()));
    assertEquals(expected, actual);
  }

  private FactTypeEntity.FactObjectBindingDefinition getBindingForSourceObjectTypeID(
          Set<FactTypeEntity.FactObjectBindingDefinition> bindings, UUID sourceObjectTypeID) {
    return bindings.stream()
            .filter(b -> Objects.equals(b.getSourceObjectTypeID(), sourceObjectTypeID))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
  }

}
