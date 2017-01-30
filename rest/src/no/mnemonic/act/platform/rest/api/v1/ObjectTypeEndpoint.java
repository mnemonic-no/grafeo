package no.mnemonic.act.platform.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.act.platform.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.api.AbstractEndpoint;
import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/v1/objectType")
@Api(tags = {"experimental"})
public class ObjectTypeEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public ObjectTypeEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve an ObjectType by its UUID.",
          notes = "This operation returns an ObjectType identified by its UUID.",
          response = ObjectType.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Requested ObjectType does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getObjectTypeById(
          @PathParam("id") @ApiParam(value = "UUID of the requested ObjectType.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getObjectType(getHeader(), new GetObjectTypeByIdRequest().setId(id)));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "List available ObjectTypes.",
          notes = "This operation returns all available ObjectTypes.",
          response = ObjectType.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchObjectTypes()
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectTypes(getHeader(), new SearchObjectTypeRequest()));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Create a new ObjectType.",
          notes = "This operation creates a new ObjectType in the Namespace of the running instance.",
          response = ObjectType.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response createObjectType(
          @ApiParam(value = "Request to create ObjectType.") @NotNull @Valid CreateObjectTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createObjectType(getHeader(), request))
            .buildResponse();
  }

  @PUT
  @Path("/uuid/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Update an existing ObjectType.",
          notes = "This operation updates an existing ObjectType.",
          response = ObjectType.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "ObjectType does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response updateObjectType(
          @PathParam("id") @ApiParam(value = "UUID of ObjectType.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to update ObjectType.") @NotNull @Valid UpdateObjectTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateObjectType(getHeader(), request.setId(id)));
  }

}
