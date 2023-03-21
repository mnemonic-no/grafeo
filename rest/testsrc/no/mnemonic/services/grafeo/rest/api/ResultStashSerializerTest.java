package no.mnemonic.services.grafeo.rest.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.rest.providers.ObjectMapperResolver;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResultStashSerializerTest {

  private static final ObjectMapper mapper = ObjectMapperResolver.getInstance();

  @Test
  public void testSerializationOfNumericFields() throws Exception {
    String json = toJson(ResultStash.builder()
            .setStatus(Response.Status.OK)
            .setCount(25)
            .setLimit(100)
            .buildResponse());
    JsonNode result = mapper.readTree(json);

    assertTrue(result.isObject());
    assertEquals(200, result.get("responseCode").asInt());
    assertEquals(25, result.get("count").asInt());
    assertEquals(100, result.get("limit").asInt());
  }

  @Test
  public void testSerializationOfActionError() throws Exception {
    String json = toJson(ResultStash.builder()
            .addActionError("message", "template", "field", "parameter")
            .buildResponse());
    JsonNode result = mapper.readTree(json);

    assertTrue(result.get("messages").isArray());
    assertEquals(1, result.get("messages").size());
    assertEquals("ActionError", result.get("messages").get(0).get("type").asText());
    assertEquals("message", result.get("messages").get(0).get("message").asText());
    assertEquals("template", result.get("messages").get(0).get("messageTemplate").asText());
    assertEquals("field", result.get("messages").get(0).get("field").asText());
    assertEquals("parameter", result.get("messages").get(0).get("parameter").asText());
    assertTrue(result.get("messages").get(0).get("timestamp").isTextual());
  }

  @Test
  public void testSerializationOfFieldError() throws Exception {
    String json = toJson(ResultStash.builder()
            .addFieldError("message", "template", "field", "parameter")
            .buildResponse());
    JsonNode result = mapper.readTree(json);

    assertTrue(result.get("messages").isArray());
    assertEquals(1, result.get("messages").size());
    assertEquals("FieldError", result.get("messages").get(0).get("type").asText());
    assertEquals("message", result.get("messages").get(0).get("message").asText());
    assertEquals("template", result.get("messages").get(0).get("messageTemplate").asText());
    assertEquals("field", result.get("messages").get(0).get("field").asText());
    assertEquals("parameter", result.get("messages").get(0).get("parameter").asText());
    assertTrue(result.get("messages").get(0).get("timestamp").isTextual());
  }

  @Test
  public void testSerializationOfSingleObject() throws Exception {
    String json = toJson(ResultStash.builder()
            .setData(Fact.builder().setId(UUID.randomUUID()).build())
            .buildResponse());
    JsonNode result = mapper.readTree(json);

    assertTrue(result.get("data").isObject());
    assertEquals(0, result.get("size").asInt());
  }

  @Test
  public void testSerializationOfList() throws Exception {
    String json = toJson(ResultStash.builder()
            .setData(ListUtils.list(Fact.builder().setId(UUID.randomUUID()).build()))
            .buildResponse());
    JsonNode result = mapper.readTree(json);

    assertTrue(result.get("data").isArray());
    assertEquals(1, result.get("data").size());
    assertEquals(1, result.get("size").asInt());
  }

  @Test
  public void testSerializationOfSet() throws Exception {
    String json = toJson(ResultStash.builder()
            .setData(SetUtils.set(Fact.builder().setId(UUID.randomUUID()).build()))
            .buildResponse());
    JsonNode result = mapper.readTree(json);

    assertTrue(result.get("data").isArray());
    assertEquals(1, result.get("data").size());
    assertEquals(1, result.get("size").asInt());
  }

  @Test
  public void testSerializationOfResultSet() throws Exception {
    String json = toJson(ResultStash.builder()
            .setData(StreamingResultSet.<Fact>builder()
                    .setValues(ListUtils.list(Fact.builder().setId(UUID.randomUUID()).build()))
                    .build())
            .buildResponse());
    JsonNode result = mapper.readTree(json);

    assertTrue(result.get("data").isArray());
    assertEquals(1, result.get("data").size());
    assertEquals(1, result.get("size").asInt());
  }

  private String toJson(Response response) throws Exception {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ((StreamingOutput) response.getEntity()).write(baos);
      return baos.toString();
    }
  }
}
