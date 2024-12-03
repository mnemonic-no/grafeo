package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NamespaceTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeNamespace() {
    Namespace namespace = Namespace.builder()
            .setId(UUID.randomUUID())
            .setName("namespace")
            .build();

    JsonNode root = mapper.valueToTree(namespace);
    assertEquals(namespace.getId().toString(), root.get("id").textValue());
    assertEquals(namespace.getName(), root.get("name").textValue());
  }

}
