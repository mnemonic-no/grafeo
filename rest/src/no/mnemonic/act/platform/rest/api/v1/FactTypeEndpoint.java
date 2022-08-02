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
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.act.platform.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static no.mnemonic.act.platform.rest.api.ResultStash.buildResponse;

@Path("/v1/factType")
@Api(tags = {"development"})
public class FactTypeEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final ThreatIntelligenceService service;

  @Inject
  public FactTypeEndpoint(CredentialsResolver credentialsResolver, ThreatIntelligenceService service) {
    this.credentialsResolver = credentialsResolver;
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
  @RolesAllowed("viewThreatIntelType")
  public Response getFactTypeById(
          @PathParam("id") @ApiParam(value = "UUID of the requested FactType.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactType(credentialsResolver.getRequestHeader(), new GetFactTypeByIdRequest().setId(id)));
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
  @RolesAllowed("viewThreatIntelType")
  public Response searchFactTypes()
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchFactTypes(credentialsResolver.getRequestHeader(), new SearchFactTypeRequest()));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Create a new FactType.",
          notes = "This operation creates a new FactType in the Namespace of the running instance.\n\n" +
                  "A FactType defines what kind of Facts can be created and specifies either that a Fact links to Objects " +
                  "or that a Fact directly references another Fact (i.e. a meta Fact). The first kind of FactType is " +
                  "defined using the 'relevantObjectBindings' field whereas the second kind of FactType using the " +
                  "'relevantFactBindings' field. It is not permitted to create a FactType which allows bindings between " +
                  "both Objects and Facts for the same FactType.\n\n" +
                  "Whenever a new Fact is created the system verifies that the Fact satisfies the FactType definition by " +
                  "checking that the Fact's value passes the FactType's Validator and that the Fact binds to Objects or " +
                  "Facts of the correct type.\n\n" +
                  "The following Validators exist:\n\n" +
                  "* TrueValidator: It accepts a value without any validation. Use carefully as the Validator cannot be " +
                  "changed after the FactType has been created!\n" +
                  "* RegexValidator: It matches a value against a regular expression. The regular expression must be " +
                  "provided using the 'validatorParameter' field.\n" +
                  "* NullValidator: It enforces that the value of a Fact is unset (null).",
          response = FactType.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addThreatIntelType")
  public Response createFactType(
          @ApiParam(value = "Request to create FactType.") @NotNull @Valid CreateFactTypeRequest request
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
  @ApiOperation(
          value = "Update an existing FactType.",
          notes = "This operation updates an existing FactType.\n\n" +
                  "It is only possible to add more allowed bindings between a Fact and Objects ('addObjectBindings' field), " +
                  "or between a Fact and other Facts ('addFactBindings' field). It is not allowed to specify bindings " +
                  "between both Objects and Facts for the same FactType and it is not possible to remove existing bindings. " +
                  "This means that a FactType can be opened up by allowing more bindings but that it cannot become more " +
                  "restrictive (removing previously allowed bindings).",
          response = FactType.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "FactType does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("updateThreatIntelType")
  public Response updateFactType(
          @PathParam("id") @ApiParam(value = "UUID of FactType.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to update FactType.") @NotNull @Valid UpdateFactTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateFactType(credentialsResolver.getRequestHeader(), request.setId(id)));
  }

}
