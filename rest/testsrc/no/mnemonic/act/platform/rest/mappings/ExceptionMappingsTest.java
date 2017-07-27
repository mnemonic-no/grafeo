package no.mnemonic.act.platform.rest.mappings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.exceptions.*;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.api.request.v1.Direction;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import no.mnemonic.act.platform.rest.api.ResultMessage;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.act.platform.rest.api.ResultMessage.Type.ActionError;
import static no.mnemonic.act.platform.rest.api.ResultMessage.Type.FieldError;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

/**
 * Test exception mappings with one specific endpoint, however, it should work with every endpoint.
 */
public class ExceptionMappingsTest extends AbstractEndpointTest {

  @Test
  public void testAccessDeniedMapperReturns403() throws Exception {
    Response response = executeRequest(new AccessDeniedException("message"));
    assertEquals(403, response.getStatus());
    assertMessages(getMessages(response), "message", "access.denied");
  }

  @Test
  public void testAuthenticationFailedMapperReturns401() throws Exception {
    Response response = executeRequest(new AuthenticationFailedException("message"));
    assertEquals(401, response.getStatus());
    assertMessages(getMessages(response), "message", "user.not.authenticated");
  }

  @Test
  public void testUnexpectedAuthenticationFailedMapperReturns401() throws Exception {
    Response response = executeRequest(new UnexpectedAuthenticationFailedException("message"));
    assertEquals(401, response.getStatus());
    assertMessages(getMessages(response), "message", "user.not.authenticated");
  }

  @Test
  public void testInvalidArgumentMapperReturns412() throws Exception {
    Response response = executeRequest(new InvalidArgumentException().addValidationError("message", "template", "property", "value"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), FieldError);
  }

  @Test
  public void testFallbackExceptionMapperReturns500() throws Exception {
    Response response = executeRequest(new RuntimeException());
    assertEquals(500, response.getStatus());
    assertMessages(getMessages(response), "An unknown server error occurred.", "unknown.server.error");
  }

  @Test
  public void testObjectNotFoundMapperReturns404() throws Exception {
    Response response = executeRequest(new ObjectNotFoundException("message", "template", "property", "value"));
    assertEquals(404, response.getStatus());
    assertMessages(getMessages(response), ActionError);
  }

  @Test
  public void testFailedRequestValidationReturns412() throws Exception {
    CreateFactRequest request = new CreateFactRequest()
            .setType("type")
            .addBinding(new CreateFactRequest.FactObjectBinding().setDirection(Direction.None));
    Response response = target("/v1/fact").request().post(Entity.json(request));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "may not be null", "{javax.validation.constraints.NotNull.message}", "value", "NULL");
  }

  @Test
  public void testFailedRequestValidationNestedReturns412() throws Exception {
    CreateFactRequest request = new CreateFactRequest()
            .setType("type")
            .setValue("value")
            .addBinding(new CreateFactRequest.FactObjectBinding());
    Response response = target("/v1/fact").request().post(Entity.json(request));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "may not be null", "{javax.validation.constraints.NotNull.message}", "bindings[0].direction", "NULL");
  }

  @Test
  public void testFailedRequestValidationWithNullRequestReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json(null));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "may not be null", "{javax.validation.constraints.NotNull.message}", "request", "NULL");
  }

  @Test
  public void testInvalidJsonRequestReturns400() throws Exception {
    List<String> requests = ListUtils.list(
            "{\"value",
            "{\"value : \"something\"}",
            "{\"value\" : \"something"
    );

    for (String request : requests) {
      Response response = target("/v1/fact").request().post(Entity.json(request));
      assertEquals(400, response.getStatus());
      assertMessages(getMessages(response), "Invalid JSON request received.", "invalid.json.request");
    }
  }

  @Test
  public void testUnknownFieldMappingReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json("{\"unknown\" : \"something\"}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "Unknown JSON field detected.", "unknown.json.field", "unknown", "");
  }

  @Test
  public void testUnknownFieldMappingNestedReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json("{\"bindings\" : [{\"unknown\" : \"something\"}]}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "Unknown JSON field detected.", "unknown.json.field", "bindings[0].unknown", "");
  }

  @Test
  public void testFieldParsingErrorReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json("{\"source\" : \"something\"}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "Invalid JSON field detected.", "invalid.json.field", "source", "something");
  }

  @Test
  public void testFieldParsingErrorWithWrongEnumReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json("{\"bindings\" : [{\"direction\" : \"something\"}]}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "Invalid JSON field detected.", "invalid.json.field", "bindings[0].direction", "something");
  }

  @Test
  public void testUrlNotFoundReturns404() throws Exception {
    Response response = target("/not/existing").request().get();
    assertEquals(404, response.getStatus());
    assertMessages(getMessages(response), "Requested URL does not exist.", "url.not.exist");
  }

  @Test
  public void testInvalidUrlParameterReturns412() throws Exception {
    Response response = target("/v1/fact/uuid/invalid").request().get();
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "Invalid URL parameter detected.", "invalid.url.parameter");
  }

  private void assertMessages(ArrayNode messages, ResultMessage.Type type) {
    assertEquals(1, messages.size());
    assertEquals(type.name(), messages.get(0).get("type").asText());
    assertMessage(messages.get(0), "message", "template", "property", "value");
  }

  private void assertMessages(ArrayNode messages, String message, String template) {
    assertEquals(1, messages.size());
    assertEquals(ActionError.name(), messages.get(0).get("type").asText());
    assertEquals(message, messages.get(0).get("message").asText());
    assertEquals(template, messages.get(0).get("messageTemplate").asText());
  }

  private void assertMessages(ArrayNode messages, String message, String template, String property, String value) {
    assertEquals(1, messages.size());
    assertEquals(FieldError.name(), messages.get(0).get("type").asText());
    assertMessage(messages.get(0), message, template, property, value);
  }


  private void assertMessage(JsonNode jsonMessage, String message, String template, String property, String value) {
    assertEquals(message, jsonMessage.get("message").asText());
    assertEquals(template, jsonMessage.get("messageTemplate").asText());
    assertEquals(property, jsonMessage.get("field").asText());
    assertEquals(value, jsonMessage.get("parameter").asText());
  }

  private Response executeRequest(Throwable ex) throws Exception {
    when(getTiService().getFact(any(), isA(GetFactByIdRequest.class))).thenThrow(ex);
    return target("/v1/fact/uuid/" + UUID.randomUUID()).request().get();
  }

}
