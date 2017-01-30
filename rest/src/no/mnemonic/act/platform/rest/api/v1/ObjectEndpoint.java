package no.mnemonic.act.platform.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.api.AbstractEndpoint;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/v1/object")
@Api(tags = {"experimental"})
public class ObjectEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public ObjectEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve an Object by its UUID.",
          notes = "This operation returns an Object identified by its UUID. The result includes statistics about the " +
                  "Facts linked to the requested Object. The request will be rejected with a 403 if a user does not have " +
                  "access to any Facts linked to the requested Object.",
          response = Object.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Requested Object does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getObjectById(
          @PathParam("id") @ApiParam(value = "UUID of the requested Object.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getObject(getHeader(), new GetObjectByIdRequest().setId(id)));
  }

  @GET
  @Path("/{type}/{value}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve an Object by its type and value.",
          notes = "This operation returns an Object identified by its ObjectType and its value. The result includes " +
                  "statistics about the Facts linked to the requested Object. The requested type name needs to be " +
                  "globally unique, otherwise the Object needs to be fetched by it's UUID. The request will be rejected " +
                  "with a 403 if a user does not have access to any Facts linked to the requested Object.",
          response = Object.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Requested Object does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getObjectByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of the requested Object.") @NotNull @Size(min = 1) String type,
          @PathParam("value") @ApiParam(value = "Value of the requested Object.") @NotNull @Size(min = 1) String value
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getObject(getHeader(), new GetObjectByTypeValueRequest().setType(type).setValue(value)));
  }

  @POST
  @Path("/uuid/{id}/facts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve Facts bound to a specific Object.",
          notes = "This operation returns the Facts linked to a specific Object which is identified by its UUID. " +
                  "With the request body the user can specify which Facts will be included in the result. Only the " +
                  "Facts a user has access to will be returned. The request will be rejected with a 403 if a user " +
                  "does not have access to any Facts linked to the requested Object.",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Object does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchObjectFactsById(
          @PathParam("id") @ApiParam(value = "UUID of Object.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.searchObjectFacts(getHeader(), request.setObjectID(id)));
  }

  @POST
  @Path("/{type}/{value}/facts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve Facts bound to a specific Object.",
          notes = "This operation returns the Facts linked to a specific Object which is identified by its ObjectType " +
                  "and its value. With the request body the user can specify which Facts will be included in the result. " +
                  "Only the Facts a user has access to will be returned. The request will be rejected with a 403 if " +
                  "a user does not have access to any Facts linked to the requested Object.",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Object does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchObjectFactsByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of Object.") @NotNull @Size(min = 1) String type,
          @PathParam("value") @ApiParam(value = "Value of Object.") @NotNull @Size(min = 1) String value,
          @ApiParam(value = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.searchObjectFacts(getHeader(), request.setObjectType(type).setObjectValue(value)));
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Search for Objects.",
          notes = "This operation searches for Objects and returns any matching Objects. The result includes statistics " +
                  "about the Facts linked to the returned Objects. With the request body the user can specify which " +
                  "Objects will be returned. Searching against linked Facts will only be performed one level deep, " +
                  "i.e. only Facts directly linked to an Object will be searched. The result will be restricted to the " +
                  "Objects and Facts a user has access to. In order to return an Object a user needs to have access to " +
                  "at least one Fact linked to the Object.",
          response = Object.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response searchObjects(
          @ApiParam(value = "Request to search for Objects.") @NotNull @Valid SearchObjectRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjects(getHeader(), request));
  }

}
