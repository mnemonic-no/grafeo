package no.mnemonic.act.platform.rest.mappings;

import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import no.mnemonic.act.platform.rest.api.ResultMessage;
import org.junit.Test;

import javax.ws.rs.core.Response;

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

  private void assertMessages(ArrayNode messages, ResultMessage.Type type) {
    assertEquals(1, messages.size());
    assertEquals(type.name(), messages.get(0).get("type").asText());
    assertEquals("message", messages.get(0).get("message").asText());
    assertEquals("template", messages.get(0).get("messageTemplate").asText());
    assertEquals("property", messages.get(0).get("field").asText());
    assertEquals("value", messages.get(0).get("parameter").asText());
  }

  private void assertMessages(ArrayNode messages, String message, String template) {
    assertEquals(1, messages.size());
    assertEquals(ActionError.name(), messages.get(0).get("type").asText());
    assertEquals(message, messages.get(0).get("message").asText());
    assertEquals(template, messages.get(0).get("messageTemplate").asText());
  }

  private Response executeRequest(Throwable ex) throws Exception {
    when(getTiService().getObject(any(), isA(GetObjectByTypeValueRequest.class))).thenThrow(ex);
    return target("/v1/object/ip/1.1.1.1").request().get();
  }

}
