package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EvidenceSubmissionTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeEvidenceSubmission() {
    EvidenceSubmission submission = EvidenceSubmission.builder()
            .setId(UUID.randomUUID())
            .setName("submission")
            .setDataType("dataType")
            .setMediaType("mediaType")
            .setLength(42)
            .setTimestamp("timestamp")
            .setObservationTimestamp("observation")
            .setSource(Source.builder().setId(UUID.randomUUID()).setName("source").build().toInfo())
            .setAccessMode(AccessMode.Explicit)
            .setChecksum("checksum")
            .build();

    JsonNode root = mapper.valueToTree(submission);
    assertEquals(submission.getId().toString(), root.get("id").textValue());
    assertEquals(submission.getName(), root.get("name").textValue());
    assertEquals(submission.getDataType(), root.get("dataType").textValue());
    assertEquals(submission.getMediaType(), root.get("mediaType").textValue());
    assertEquals(submission.getLength(), root.get("length").longValue());
    assertEquals(submission.getTimestamp(), root.get("timestamp").textValue());
    assertEquals(submission.getObservationTimestamp(), root.get("observationTimestamp").textValue());
    assertTrue(root.get("source").isObject());
    assertEquals(submission.getAccessMode().toString(), root.get("accessMode").textValue());
    assertEquals(submission.getChecksum(), root.get("checksum").textValue());
  }

}
