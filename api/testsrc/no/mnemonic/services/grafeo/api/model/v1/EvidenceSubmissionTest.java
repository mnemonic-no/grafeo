package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EvidenceSubmissionTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeEvidenceSubmission() {
    EvidenceSubmission submission = EvidenceSubmission.builder()
            .setId(UUID.randomUUID())
            .setName("submission")
            .setDataType("dataType")
            .setMediaType("mediaType")
            .setLength(42)
            .setTimestamp(1480520821000L)
            .setObservationTimestamp(1480520822000L)
            .setOrigin(Origin.builder().setId(UUID.randomUUID()).setName("origin").build().toInfo())
            .setAccessMode(AccessMode.Explicit)
            .setChecksum("checksum")
            .build();

    JsonNode root = mapper.valueToTree(submission);
    assertEquals(submission.getId().toString(), root.get("id").textValue());
    assertEquals(submission.getName(), root.get("name").textValue());
    assertEquals(submission.getDataType(), root.get("dataType").textValue());
    assertEquals(submission.getMediaType(), root.get("mediaType").textValue());
    assertEquals(submission.getLength(), root.get("length").longValue());
    assertEquals("2016-11-30T15:47:01Z", root.get("timestamp").textValue());
    assertEquals("2016-11-30T15:47:02Z", root.get("observationTimestamp").textValue());
    assertTrue(root.get("origin").isObject());
    assertEquals(submission.getAccessMode().toString(), root.get("accessMode").textValue());
    assertEquals(submission.getChecksum(), root.get("checksum").textValue());
  }

}
