package no.mnemonic.act.platform.rest.features;

import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

/**
 * Test CORS with one specific endpoint, however, it should work with every endpoint.
 */
public class CorsFeatureTest extends AbstractEndpointTest {

  @Before
  public void setUp() throws Exception {
    when(getTiService().getFact(any(), isA(GetFactByIdRequest.class))).thenReturn(Fact.builder().build());
  }

  @Test
  public void testSuccessfulNonCrossOriginRequest() {
    Response response = target("/v1/fact/uuid/" + UUID.randomUUID()).request()
            .header("ACT-User-ID", 1)
            .get();
    assertEquals(200, response.getStatus());
    assertFalse(response.getHeaders().containsKey("Access-Control-Allow-Origin"));
  }

  @Test
  public void testSuccessfulCrossOriginRequest() {
    String allowedOrigin = "http://www.example.org";

    Response response = target("/v1/fact/uuid/" + UUID.randomUUID()).request()
            .header("ACT-User-ID", 1)
            .header("origin", allowedOrigin)
            .get();
    assertEquals(200, response.getStatus());
    assertEquals(allowedOrigin, response.getHeaders().getFirst("Access-Control-Allow-Origin"));
  }

  @Test
  public void testFailedCrossOriginRequest() {
    Response response = target("/v1/fact/uuid/" + UUID.randomUUID()).request()
            .header("ACT-User-ID", 1)
            .header("origin", "http://www.evil.com")
            .get();
    assertEquals(403, response.getStatus());
  }

}
