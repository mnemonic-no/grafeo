package no.mnemonic.services.grafeo.rest.mappings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ServiceTimeOutException;
import no.mnemonic.services.grafeo.api.exceptions.*;
import no.mnemonic.services.grafeo.api.request.v1.*;
import no.mnemonic.services.grafeo.rest.AbstractEndpointTest;
import no.mnemonic.services.grafeo.rest.api.ResultMessage;
import org.junit.jupiter.api.Test;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultMessage.Type.ActionError;
import static no.mnemonic.services.grafeo.rest.api.ResultMessage.Type.FieldError;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  public void testForbiddenMapperReturns403() throws Exception {
    Response response = executeRequest(new ForbiddenException());
    assertEquals(403, response.getStatus());
    assertMessages(getMessages(response), "Access denied to requested URL.", "access.denied");
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
  public void testFallbackExceptionMapperWithUnhandledRuntimeExceptionReturns500() throws Exception {
    Response response = executeRequest(new UnhandledRuntimeException("test"));
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
  public void testOperationTimeoutMapperReturns408() throws Exception {
    TraverseGraphRequest request = new TraverseGraphRequest()
            .setQuery("while (true) {}");
    when(getService().traverse(any(), isA(TraverseGraphByObjectsRequest.class)))
            .thenThrow(new OperationTimeoutException("message", "template"));

    Response response = target(String.format("/v1/traverse/object/%s", UUID.randomUUID())).request().post(Entity.json(request));
    assertEquals(408, response.getStatus());
    assertMessages(getMessages(response), "message", "template");
  }

  @Test
  public void testServiceTimeoutMapperReturns503() throws Exception {
    Response response = executeRequest(new ServiceTimeOutException("test", "test"));
    assertEquals(503, response.getStatus());
    assertMessages(getMessages(response), "Request timed out, service may be overloaded or unavailable. Please try again later.", "service.timeout");
  }

  @Test
  public void testFailedRequestValidationReturns412() throws Exception {
    CreateFactRequest request = new CreateFactRequest();
    Response response = target("/v1/fact").request().post(Entity.json(request));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "must not be blank", "{javax.validation.constraints.NotBlank.message}", "type", "NULL");
  }

  @Test
  public void testFailedRequestValidationNestedReturns412() throws Exception {
    CreateFactTypeRequest request = new CreateFactTypeRequest()
            .setName("name")
            .setValidator("validator")
            .addRelevantFactBinding(new MetaFactBindingDefinition());
    Response response = target("/v1/factType").request().post(Entity.json(request));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "must not be null", "{javax.validation.constraints.NotNull.message}", "relevantFactBindings[0].factType", "NULL");
  }

  @Test
  public void testFailedRequestValidationWithNullRequestReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json(null));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "must not be null", "{javax.validation.constraints.NotNull.message}", "request", "NULL");
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
    Response response = target("/v1/factType").request().post(Entity.json("{\"relevantObjectBindings\" : [{\"unknown\" : \"something\"}]}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "Unknown JSON field detected.", "unknown.json.field", "relevantObjectBindings[0].unknown", "");
  }

  @Test
  public void testFieldParsingErrorReturns412() throws Exception {
    Response response = target("/v1/fact/uuid/123e4567-e89b-12d3-a456-426655440000/comments").request().post(Entity.json("{\"replyTo\" : \"something\"}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "JSON field has an invalid value.", "invalid.json.field.value", "replyTo", "something");
  }

  @Test
  public void testFieldParsingErrorWithWrongEnumReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json("{\"accessMode\" : \"something\"}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "JSON field has an invalid value.", "invalid.json.field.value", "accessMode", "something");
  }

  @Test
  public void testFieldParsingErrorNestedReturns412() throws Exception {
    Response response = target("/v1/factType").request().post(Entity.json("{\"relevantObjectBindings\" : [{\"sourceObjectType\" : \"something\"}]}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "JSON field has an invalid value.", "invalid.json.field.value", "relevantObjectBindings[0].sourceObjectType", "something");
  }

  @Test
  public void testFieldParsingErrorWithWrongTimestampReturns412() throws Exception {
    Response response = target("/v1/fact/search").request().post(Entity.json("{\"startTimestamp\" : \"something\"}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "JSON field has an invalid value.", "invalid.json.field.value", "startTimestamp", "something");
  }

  @Test
  public void testFieldParsingErrorWithWrongArrayValueReturns412() throws Exception {
    Response response = target("/v1/fact/search").request().post(Entity.json("{\"objectID\" : [\"something\"]}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "JSON field has an invalid value.", "invalid.json.field.value", "objectID[0]", "something");
  }

  @Test
  public void testFieldParsingTypeErrorReturns412() throws Exception {
    Response response = target("/v1/fact").request().post(Entity.json("{\"acl\" : \"123e4567-e89b-12d3-a456-426655440000\"}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "JSON field has an invalid type.", "invalid.json.field.type", "acl", "");
  }

  @Test
  public void testFieldParsingTypeErrorWithWrongTimestampReturns412() throws Exception {
    Response response = target("/v1/fact/search").request().post(Entity.json("{\"startTimestamp\" : true}"));
    assertEquals(412, response.getStatus());
    assertMessages(getMessages(response), "JSON field has an invalid type.", "invalid.json.field.type", "startTimestamp", "");
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
    when(getService().getFact(any(), isA(GetFactByIdRequest.class))).thenThrow(ex);
    return target("/v1/fact/uuid/" + UUID.randomUUID()).request().get();
  }

}
