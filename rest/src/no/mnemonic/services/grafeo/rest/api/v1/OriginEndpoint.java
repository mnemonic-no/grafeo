package no.mnemonic.services.grafeo.rest.api.v1;

import io.swagger.annotations.*;
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
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultStash.buildResponse;

@Path("/v1/origin")
@Api(tags = {"development"})
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
  @ApiOperation(
          value = "Retrieve an Origin by its UUID.",
          notes = "This operation returns an Origin identified by its UUID.",
          response = Origin.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Requested Origin does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelOrigin")
  public Response getOriginById(
          @PathParam("id") @ApiParam(value = "UUID of the requested Origin.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getOrigin(credentialsResolver.getRequestHeader(), new GetOriginByIdRequest().setId(id)));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "List available Origins.",
          notes = "This operation returns all available Origins.",
          response = Origin.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelOrigin")
  public Response searchOrigins(
          @QueryParam("type") @ApiParam(value = "Only return Origins having a specific type") Set<SearchOriginRequest.Type> type,
          @QueryParam("includeDeleted") @ApiParam(value = "Include deleted Origins (default false)") Boolean includeDeleted,
          @QueryParam("limit") @ApiParam(value = "Limit the number of returned Origins (default 25, 0 means all)") @Min(0) Integer limit
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
  @ApiOperation(
          value = "Create a new Origin.",
          notes = "This operation creates a new Origin in the Namespace of the running instance.\n\n" +
                  "All Facts are marked with an Origin in order to identify where the information came from. This operation " +
                  "creates an Origin of type 'Group', i.e. an Origin which is not bound to a specific user. Origins bound " +
                  "to specific users will automatically be created when Facts are added without an Origin. Then the user " +
                  "adding the Fact will become the Origin. If the request contains an organization users must hold the " +
                  "permission to create Origins for that organization.",
          response = Origin.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addThreatIntelOrigin")
  public Response createOrigin(
          @ApiParam(value = "Request to create Origin.") @NotNull @Valid CreateOriginRequest request
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
  @ApiOperation(
          value = "Update an existing Origin.",
          notes = "This operation updates an existing Origin.\n\n" +
                  "If the Origin contains an organization users must hold the permission to update Origins for that " +
                  "organization. Similarly, if the operation changes the Origin's organization users must hold the " +
                  "same permission for the new organization.",
          response = Origin.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Origin does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("updateThreatIntelOrigin")
  public Response updateOrigin(
          @PathParam("id") @ApiParam(value = "UUID of Origin.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to update Origin.") @NotNull @Valid UpdateOriginRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateOrigin(credentialsResolver.getRequestHeader(), request.setId(id)));
  }

  @DELETE
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Mark an existing Origin as deleted.",
          notes = "This operation marks an existing Origin as deleted. It is not allowed to use deleted Origins when adding new Facts.",
          response = Origin.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Origin does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("deleteThreatIntelOrigin")
  public Response deleteOrigin(
          @PathParam("id") @ApiParam(value = "UUID of Origin.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.deleteOrigin(credentialsResolver.getRequestHeader(), new DeleteOriginRequest().setId(id)));
  }
}
