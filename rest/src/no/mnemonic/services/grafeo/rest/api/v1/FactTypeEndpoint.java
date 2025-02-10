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
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.services.grafeo.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.rest.api.ResultStash;
import no.mnemonic.services.grafeo.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultStash.buildResponse;

@Path("/v1/factType")
@Tag(name = "/v1/factType")
public class FactTypeEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final GrafeoService service;

  @Inject
  public FactTypeEndpoint(CredentialsResolver credentialsResolver, GrafeoService service) {
    this.credentialsResolver = credentialsResolver;
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve a FactType by its UUID.",
          description = "This operation returns a FactType identified by its UUID."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "FactType was successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashFactType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Requested FactType does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoType")
  public Response getFactTypeById(
          @PathParam("id") @Parameter(description = "UUID of the requested FactType.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactType(credentialsResolver.getRequestHeader(), new GetFactTypeByIdRequest().setId(id)));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "List available FactTypes.",
          description = "This operation returns all available FactTypes."
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "FactTypes were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListFactType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoType")
  public Response searchFactTypes()
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchFactTypes(credentialsResolver.getRequestHeader(), new SearchFactTypeRequest()));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Create a new FactType.",
          description = """
                  This operation creates a new FactType in the Namespace of the running instance.
                  
                  A FactType defines what kind of Facts can be created and specifies either that a Fact links to Objects
                  or that a Fact directly references another Fact (i.e. a meta Fact). The first kind of FactType is
                  defined using the 'relevantObjectBindings' field whereas the second kind of FactType using the
                  'relevantFactBindings' field. It is not permitted to create a FactType which allows bindings between
                  both Objects and Facts for the same FactType.
                  
                  Whenever a new Fact is created the system verifies that the Fact satisfies the FactType definition by
                  checking that the Fact's value passes the FactType's Validator and that the Fact binds to Objects or
                  Facts of the correct type.
                  
                  The following Validators exist:
                  
                  * TrueValidator: It accepts a value without any validation. Use carefully as the Validator cannot be
                  changed after the FactType has been created!
                  * RegexValidator: It matches a value against a regular expression. The regular expression must be
                  provided using the 'validatorParameter' field.
                  * NullValidator: It enforces that the value of a Fact is unset (null).
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "FactType was successfully created.",
                  content = @Content(schema = @Schema(implementation = ResultStashFactType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addGrafeoType")
  public Response createFactType(
          @Parameter(description = "Request to create FactType.") @NotNull @Valid CreateFactTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFactType(credentialsResolver.getRequestHeader(), request))
            .buildResponse();
  }

  @PUT
  @Path("/uuid/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Update an existing FactType.",
          description = """
                  This operation updates an existing FactType.
                  
                  It is only possible to add more allowed bindings between a Fact and Objects ('addObjectBindings' field),
                  or between a Fact and other Facts ('addFactBindings' field). It is not allowed to specify bindings
                  between both Objects and Facts for the same FactType and it is not possible to remove existing bindings.
                  This means that a FactType can be opened up by allowing more bindings but that it cannot become more
                  restrictive (removing previously allowed bindings).
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "FactType was successfully updated.",
                  content = @Content(schema = @Schema(implementation = ResultStashFactType.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "FactType does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("updateGrafeoType")
  public Response updateFactType(
          @PathParam("id") @Parameter(description = "UUID of FactType.") @NotNull @Valid UUID id,
          @Parameter(description = "Request to update FactType.") @NotNull @Valid UpdateFactTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateFactType(credentialsResolver.getRequestHeader(), request.setId(id)));
  }

  private static class ResultStashFactType extends ResultStash<FactType> {
  }

  private static class ResultStashListFactType extends ResultStash<List<FactType>> {
  }
}
