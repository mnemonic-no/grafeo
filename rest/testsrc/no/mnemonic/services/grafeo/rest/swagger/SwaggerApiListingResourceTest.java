package no.mnemonic.services.grafeo.rest.swagger;

import no.mnemonic.services.grafeo.rest.AbstractEndpointTest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SwaggerApiListingResourceTest extends AbstractEndpointTest {

  @Test
  public void testFetchSwaggerJson() {
    Response response = target("/openapi.json").request().get();
    assertEquals(200, response.getStatus());
  }

}
