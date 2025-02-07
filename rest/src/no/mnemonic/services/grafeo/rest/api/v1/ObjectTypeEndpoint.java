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
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.rest.api.ResultStash;
import no.mnemonic.services.grafeo.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultStash.buildResponse;

@Path("/v1/objectType")
@Tag(name = "/v1/objectType")
public class ObjectTypeEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final GrafeoService service;

  @Inject
  public ObjectTypeEndpoint(CredentialsResolver credentialsResolver, GrafeoService service) {
    this.credentialsResolver = credentialsResolver;
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve an ObjectType by its UUID.",
          description = "This operation returns an ObjectType identified by its UUID."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "ObjectType was successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashObjectType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Requested ObjectType does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoType")
  public Response getObjectTypeById(
          @PathParam("id") @Parameter(description = "UUID of the requested ObjectType.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getObjectType(credentialsResolver.getRequestHeader(), new GetObjectTypeByIdRequest().setId(id)));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "List available ObjectTypes.",
          description = "This operation returns all available ObjectTypes."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "ObjectTypes were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListObjectType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoType")
  public Response searchObjectTypes()
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectTypes(credentialsResolver.getRequestHeader(), new SearchObjectTypeRequest()));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Create a new ObjectType.",
          description = """
                  This operation creates a new ObjectType in the Namespace of the running instance.
                  
                  An ObjectType defines what kind of Objects can exist in the system and Facts can only link to those
                  Objects. Objects are automatically created when Facts referencing them are added to the system. The
                  system verifies that a new Object satisfies the ObjectType definition by checking that the Object's
                  value passes the ObjectType's Validator.
                  
                  The following Validators exist:
                  
                  * TrueValidator: It accepts a value without any validation. Use carefully as the Validator cannot be
                  changed after the ObjectType has been created!
                  * RegexValidator: It matches a value against a regular expression. The regular expression must be
                  provided using the 'validatorParameter' field.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "ObjectType was successfully created.",
                  content = @Content(schema = @Schema(implementation = ResultStashObjectType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addGrafeoType")
  public Response createObjectType(
          @Parameter(description = "Request to create ObjectType.") @NotNull @Valid CreateObjectTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createObjectType(credentialsResolver.getRequestHeader(), request))
            .buildResponse();
  }

  @PUT
  @Path("/uuid/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Update an existing ObjectType.",
          description = "This operation updates an existing ObjectType."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "ObjectType was successfully updated.",
                  content = @Content(schema = @Schema(implementation = ResultStashObjectType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "ObjectType does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("updateGrafeoType")
  public Response updateObjectType(
          @PathParam("id") @Parameter(description = "UUID of ObjectType.") @NotNull @Valid UUID id,
          @Parameter(description = "Request to update ObjectType.") @NotNull @Valid UpdateObjectTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateObjectType(credentialsResolver.getRequestHeader(), request.setId(id)));
  }

  private static class ResultStashObjectType extends ResultStash<ObjectType> {
  }

  private static class ResultStashListObjectType extends ResultStash<List<ObjectType>> {
  }
}
