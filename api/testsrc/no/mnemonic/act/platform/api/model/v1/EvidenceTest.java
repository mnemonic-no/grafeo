package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EvidenceTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeEvidence() {
    Evidence evidence = Evidence.builder()
            .setChecksum("checksum")
            .setData("data")
            .build();

    JsonNode root = mapper.valueToTree(evidence);
    assertEquals(evidence.getChecksum(), root.get("checksum").textValue());
    assertEquals(evidence.getData(), root.get("data").textValue());
  }

}
