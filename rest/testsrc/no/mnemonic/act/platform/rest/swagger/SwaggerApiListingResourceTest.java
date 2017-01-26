package no.mnemonic.act.platform.rest.swagger;

import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class SwaggerApiListingResourceTest extends AbstractEndpointTest {

  @Test
  public void testFetchSwaggerJson() throws Exception {
    Response response = target("/swagger.json").request().get();
    assertEquals(200, response.getStatus());
  }

}
