package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FactCommentTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeFactComment() {
    FactComment comment = FactComment.builder()
            .setId(UUID.randomUUID())
            .setReplyTo(UUID.randomUUID())
            .setOrigin(Origin.builder().setId(UUID.randomUUID()).setName("origin").build().toInfo())
            .setComment("comment")
            .setTimestamp(1480520821000L)
            .build();

    JsonNode root = mapper.valueToTree(comment);
    assertEquals(comment.getId().toString(), root.get("id").textValue());
    assertEquals(comment.getReplyTo().toString(), root.get("replyTo").textValue());
    assertTrue(root.get("origin").isObject());
    assertEquals(comment.getComment(), root.get("comment").textValue());
    assertEquals("2016-11-30T15:47:01Z", root.get("timestamp").textValue());
  }

}
