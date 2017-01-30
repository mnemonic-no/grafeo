package no.mnemonic.act.platform.rest.swagger;

import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import io.swagger.models.Swagger;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

@Path("/swagger.json")
public class SwaggerApiListingResource extends BaseApiListingResource {

  private final SwaggerModelTransformer transformer;

  @Inject
  public SwaggerApiListingResource(SwaggerModelTransformer transformer) {
    this.transformer = transformer;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Generates the swagger.json file.", hidden = true)
  public Response getListing(
          @Context Application app,
          @Context ServletContext context,
          @Context ServletConfig config,
          @Context HttpHeaders headers,
          @Context UriInfo uriInfo
  ) {
    Swagger originalModel = process(app, context, config, headers, uriInfo);
    Swagger transformedModel = transformer.transform(originalModel);
    return Response.ok().entity(transformedModel).build();
  }

}
