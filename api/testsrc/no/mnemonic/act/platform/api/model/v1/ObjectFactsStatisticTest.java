package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectFactsStatisticTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeObjectFactsStatistic() {
    ObjectFactsStatistic statistic = ObjectFactsStatistic.builder()
            .setType(FactType.builder().setId(UUID.randomUUID()).setName("factType").build().toInfo())
            .setCount(42)
            .setLastAddedTimestamp("lastAdded")
            .setLastSeenTimestamp("lastSeen")
            .build();

    JsonNode root = mapper.valueToTree(statistic);
    assertTrue(root.get("type").isObject());
    assertEquals(statistic.getCount(), root.get("count").intValue());
    assertEquals(statistic.getLastAddedTimestamp(), root.get("lastAddedTimestamp").textValue());
    assertEquals(statistic.getLastSeenTimestamp(), root.get("lastSeenTimestamp").textValue());
  }

}
