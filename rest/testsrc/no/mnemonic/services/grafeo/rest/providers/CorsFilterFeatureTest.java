package no.mnemonic.services.grafeo.rest.providers;

import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.request.v1.GetFactByIdRequest;
import no.mnemonic.services.grafeo.rest.AbstractEndpointTest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

/**
 * Test CORS with one specific endpoint, however, it should work with every endpoint.
 */
public class CorsFilterFeatureTest extends AbstractEndpointTest {

  private static final String allowedOrigin = "http://www.example.org";

  @Test
  public void testSuccessfulCrossOriginRequest() throws Exception {
    when(getService().getFact(any(), isA(GetFactByIdRequest.class))).thenReturn(Fact.builder().build());

    Response response = target("/v1/fact/uuid/" + UUID.randomUUID()).request()
            .header("Grafeo-User-ID", 1)
            .header("origin", allowedOrigin)
            .get();
    assertEquals(200, response.getStatus());
    assertEquals(allowedOrigin, response.getHeaders().getFirst("Access-Control-Allow-Origin"));

    verify(getService()).getFact(any(), notNull());
  }

  @Test
  public void testSuccessfulCrossOriginRequestWithServiceException() throws Exception {
    when(getService().getFact(any(), isA(GetFactByIdRequest.class))).thenThrow(ObjectNotFoundException.class);

    Response response = target("/v1/fact/uuid/" + UUID.randomUUID()).request()
            .header("Grafeo-User-ID", 1)
            .header("origin", allowedOrigin)
            .get();
    assertEquals(404, response.getStatus());
    assertEquals(allowedOrigin, response.getHeaders().getFirst("Access-Control-Allow-Origin"));

    verify(getService()).getFact(any(), notNull());
  }

  @Test
  public void testSuccessfulNonCrossOriginRequest() throws Exception {
    when(getService().getFact(any(), isA(GetFactByIdRequest.class))).thenReturn(Fact.builder().build());

    Response response = target("/v1/fact/uuid/" + UUID.randomUUID()).request()
            .header("Grafeo-User-ID", 1)
            .get();
    assertEquals(200, response.getStatus());
    assertFalse(response.getHeaders().containsKey("Access-Control-Allow-Origin"));

    verify(getService()).getFact(any(), notNull());
  }

  @Test
  public void testFailedCrossOriginRequest() {
    Response response = target("/v1/fact/uuid/" + UUID.randomUUID()).request()
            .header("Grafeo-User-ID", 1)
            .header("origin", "http://www.evil.com")
            .get();
    assertEquals(403, response.getStatus());
    assertFalse(response.getHeaders().containsKey("Access-Control-Allow-Origin"));

    verifyNoInteractions(getService());
  }

}
