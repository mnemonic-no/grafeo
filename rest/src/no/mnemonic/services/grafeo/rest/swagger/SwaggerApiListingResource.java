package no.mnemonic.services.grafeo.rest.swagger;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.*;

@Path("/openapi.json")
@Singleton
public class SwaggerApiListingResource extends BaseOpenApiResource {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Generates the openapi.json file.", hidden = true)
  public Response getListing(
          @Context Application app,
          @Context ServletConfig config,
          @Context HttpHeaders headers,
          @Context UriInfo uriInfo
  ) throws Exception {
    return super.getOpenApi(headers, config, app, uriInfo, "json");
  }

}
