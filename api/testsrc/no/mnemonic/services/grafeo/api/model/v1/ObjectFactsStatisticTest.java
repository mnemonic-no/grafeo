package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectFactsStatisticTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeObjectFactsStatistic() {
    ObjectFactsStatistic statistic = ObjectFactsStatistic.builder()
            .setType(FactType.builder().setId(UUID.randomUUID()).setName("factType").build().toInfo())
            .setCount(42)
            .setLastAddedTimestamp(1480520821000L)
            .setLastSeenTimestamp(1480520822000L)
            .build();

    JsonNode root = mapper.valueToTree(statistic);
    assertTrue(root.get("type").isObject());
    assertEquals(statistic.getCount(), root.get("count").intValue());
    assertEquals("2016-11-30T15:47:01Z", root.get("lastAddedTimestamp").textValue());
    assertEquals("2016-11-30T15:47:02Z", root.get("lastSeenTimestamp").textValue());
  }

}
