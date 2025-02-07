package no.mnemonic.services.grafeo.rest.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.request.v1.*;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.rest.api.ResultStash;
import no.mnemonic.services.grafeo.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultStash.buildResponse;

@Path("/v1/origin")
@Tag(name = "/v1/origin")
public class OriginEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final GrafeoService service;

  @Inject
  public OriginEndpoint(CredentialsResolver credentialsResolver, GrafeoService service) {
    this.credentialsResolver = credentialsResolver;
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve an Origin by its UUID.",
          description = "This operation returns an Origin identified by its UUID."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Origin was successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashOrigin.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Requested Origin does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoOrigin")
  public Response getOriginById(
          @PathParam("id") @Parameter(description = "UUID of the requested Origin.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getOrigin(credentialsResolver.getRequestHeader(), new GetOriginByIdRequest().setId(id)));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "List available Origins.",
          description = "This operation returns all available Origins."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Origins were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListOrigin.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoOrigin")
  public Response searchOrigins(
          @QueryParam("type") @Parameter(description = "Only return Origins having a specific type") Set<SearchOriginRequest.Type> type,
          @QueryParam("includeDeleted") @Parameter(description = "Include deleted Origins (default false)") Boolean includeDeleted,
          @QueryParam("limit") @Parameter(description = "Limit the number of returned Origins (default 25, 0 means all)") @Min(0) Integer limit
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchOrigins(credentialsResolver.getRequestHeader(), new SearchOriginRequest()
            .setType(type)
            .setIncludeDeleted(includeDeleted)
            .setLimit(limit)
    ));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Create a new Origin.",
          description = """
                  This operation creates a new Origin in the Namespace of the running instance.
                  
                  All Facts are marked with an Origin in order to identify where the information came from. This operation
                  creates an Origin of type 'Group', i.e. an Origin which is not bound to a specific user. Origins bound
                  to specific users will automatically be created when Facts are added without an Origin. Then the user
                  adding the Fact will become the Origin. If the request contains an organization users must hold the
                  permission to create Origins for that organization.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "Origin was successfully created.",
                  content = @Content(schema = @Schema(implementation = ResultStashOrigin.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addGrafeoOrigin")
  public Response createOrigin(
          @Parameter(description = "Request to create Origin.") @NotNull @Valid CreateOriginRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createOrigin(credentialsResolver.getRequestHeader(), request))
            .buildResponse();
  }

  @PUT
  @Path("/uuid/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Update an existing Origin.",
          description = """
                  This operation updates an existing Origin.
                  
                  If the Origin contains an organization users must hold the permission to update Origins for that
                  organization. Similarly, if the operation changes the Origin's organization users must hold the
                  same permission for the new organization.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Origin was successfully updated.",
                  content = @Content(schema = @Schema(implementation = ResultStashOrigin.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Origin does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("updateGrafeoOrigin")
  public Response updateOrigin(
          @PathParam("id") @Parameter(description = "UUID of Origin.") @NotNull @Valid UUID id,
          @Parameter(description = "Request to update Origin.") @NotNull @Valid UpdateOriginRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateOrigin(credentialsResolver.getRequestHeader(), request.setId(id)));
  }

  @DELETE
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Mark an existing Origin as deleted.",
          description = "This operation marks an existing Origin as deleted. It is not allowed to use deleted Origins when adding new Facts."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Origin was successfully deleted.",
                  content = @Content(schema = @Schema(implementation = ResultStashOrigin.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Origin does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("deleteGrafeoOrigin")
  public Response deleteOrigin(
          @PathParam("id") @Parameter(description = "UUID of Origin.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.deleteOrigin(credentialsResolver.getRequestHeader(), new DeleteOriginRequest().setId(id)));
  }

  private static class ResultStashOrigin extends ResultStash<Origin> {
  }

  private static class ResultStashListOrigin extends ResultStash<List<Origin>> {
  }
}
