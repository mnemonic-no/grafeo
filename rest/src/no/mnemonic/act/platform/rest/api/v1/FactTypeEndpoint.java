package no.mnemonic.act.platform.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
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

@Path("/v1/factType")
@Api(tags = {"experimental"})
public class FactTypeEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public FactTypeEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve a FactType by its UUID.",
          notes = "This operation returns a FactType identified by its UUID.",
          response = FactType.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Requested FactType does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getFactTypeById(
          @PathParam("id") @ApiParam(value = "UUID of the requested FactType.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactType(getHeader(), new GetFactTypeByIdRequest().setId(id)));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "List available FactTypes.",
          notes = "This operation returns all available FactTypes.",
          response = FactType.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchFactTypes()
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchFactTypes(getHeader(), new SearchFactTypeRequest()));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Create a new FactType.",
          notes = "This operation creates a new FactType in the Namespace of the running instance. If bindings between " +
                  "ObjectTypes are specified ('relevantObjectBindings' field) the new FactType describes to which Objects " +
                  "a new Fact of this type can be linked. If bindings between FactTypes are specified ('relevantFactBindings' " +
                  "field) the new FactType describes a meta FactType, i.e. to which Facts a new meta Fact of this type " +
                  "can be linked. It is not allowed to specify bindings between both ObjectTypes and FactTypes for the " +
                  "same new FactType.",
          response = FactType.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response createFactType(
          @ApiParam(value = "Request to create FactType.") @NotNull @Valid CreateFactTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFactType(getHeader(), request))
            .buildResponse();
  }

  @PUT
  @Path("/uuid/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Update an existing FactType.",
          notes = "This operation updates an existing FactType. It is only possible to add new bindings between the " +
                  "FactType and other ObjectTypes ('addObjectBindings' field), or between the FactType and other " +
                  "FactTypes ('addFactBindings' field). It is not allowed to specify bindings between both ObjectTypes " +
                  "and FactTypes and it is not allowed to remove existing bindings.",
          response = FactType.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "FactType does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response updateFactType(
          @PathParam("id") @ApiParam(value = "UUID of FactType.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to update FactType.") @NotNull @Valid UpdateFactTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateFactType(getHeader(), request.setId(id)));
  }

}
