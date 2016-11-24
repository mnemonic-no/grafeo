package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FactCommentTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeFactComment() {
    FactComment comment = FactComment.builder()
            .setId(UUID.randomUUID())
            .setReplyTo(UUID.randomUUID())
            .setSource(Source.builder().setId(UUID.randomUUID()).setName("source").build().toInfo())
            .setComment("comment")
            .setTimestamp("timestamp")
            .build();

    JsonNode root = mapper.valueToTree(comment);
    assertEquals(comment.getId().toString(), root.get("id").textValue());
    assertEquals(comment.getReplyTo().toString(), root.get("replyTo").textValue());
    assertTrue(root.get("source").isObject());
    assertEquals(comment.getComment(), root.get("comment").textValue());
    assertEquals(comment.getTimestamp(), root.get("timestamp").textValue());
  }

}
