package no.mnemonic.services.grafeo.rest.swagger;

import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import io.swagger.models.Swagger;
import no.mnemonic.services.common.documentation.swagger.SwaggerModelTransformer;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.util.concurrent.atomic.AtomicReference;

@Path("/swagger.json")
@Singleton
public class SwaggerApiListingResource extends BaseApiListingResource {

  private final SwaggerModelTransformer transformer;
  private final AtomicReference<Swagger> cachedSwagger = new AtomicReference<>();

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
    // The generated and transformed Swagger model is cached in order to avoid running the transformations multiple times
    // on the same model because Swagger caches the transformed model internally. The model won't change anyways.
    Swagger swagger = cachedSwagger.updateAndGet(s -> {
      if (s != null) return s;
      return transformer.transform(process(app, context, config, headers, uriInfo));
    });
    return Response.ok().entity(swagger).build();
  }

}
