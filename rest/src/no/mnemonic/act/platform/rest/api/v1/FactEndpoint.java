package no.mnemonic.act.platform.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.api.AbstractEndpoint;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Path("/v1/fact")
@Api(tags = {"experimental"})
public class FactEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public FactEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve a Fact by its UUID.",
          notes = "This operation returns a Fact identified by its UUID. The result includes the Objects directly " +
                  "linked to the requested Fact. The request will be rejected with a 403 if a user does not have " +
                  "access to the requested Fact.\n\n" +
                  "If the accessMode is 'Public' the Fact will be available to everyone. If the accessMode is " +
                  "'Explicit' only users in the Fact's ACL will have access to the Fact. If the accessMode is " +
                  "'RoleBased' (the default mode) a user must be either in the Fact's ACL or have general role-based " +
                  "access to the Organization owning the Fact. A user who created a Fact will always have access to it.",
          response = Fact.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Requested Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getFactById(
          @PathParam("id") @ApiParam(value = "UUID of the requested Fact.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFact(getHeader(), new GetFactByIdRequest().setId(id)));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Create a new Fact.",
          notes = "This operation creates and returns a new Fact. The new Fact must conform to the specified FactType, " +
                  "i.e. the value must pass the FactType's Validator and the binding to Objects must respect the " +
                  "definition by the FactType. The new Fact will be saved using the FactType's EntityHandler.\n\n" +
                  "If the new Fact links to an Object which does not exist yet the missing Object will be created " +
                  "automatically with respect to the Object's ObjectType (need to pass the ObjectType's Validator and " +
                  "stored using the ObjectType's EntityHandler).\n\n" +
                  "If a Fact with the same type, value, organization, source, accessMode, confidenceLevel and bound Objects " +
                  "already exists, no new Fact will be created. Instead the lastSeenTimestamp of the existing Fact will be updated.",
          response = Fact.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response createFact(
          @ApiParam(value = "Request to create Fact.") @NotNull @Valid CreateFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFact(getHeader(), request))
            .buildResponse();
  }

  @POST
  @Path("/uuid/{fact}/retract")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retract an existing Fact.",
          notes = "This operation retracts an already existing Fact. It creates a new Fact with a special 'retract' " +
                  "FactType which references the retracted Fact. The request will be rejected with a 403 if a user does " +
                  "not have access to the Fact to retract.",
          response = Fact.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact to retract does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response retractFact(
          @PathParam("fact") @ApiParam(value = "UUID of Fact to retract.") @NotNull @Valid UUID fact,
          @ApiParam(value = "Request to retract a Fact.") @NotNull @Valid RetractFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.retractFact(getHeader(), request.setFact(fact)))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/access")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve a Fact's ACL.",
          notes = "This operation retrieves the Access Control List of a Fact, i.e. the list of who has access to a " +
                  "Fact. The request will be rejected with a 403 if a user does not have access to the Fact.",
          response = AclEntry.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getFactAcl(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactAcl(getHeader(), new GetFactAclRequest().setFact(fact)));
  }

  @POST
  @Path("/uuid/{fact}/access/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Grant a Subject access to a Fact.",
          notes = "This operation grants a Subject access to a Fact. The request will be rejected with a 403 " +
                  "if a user does not have access to the Fact.",
          response = AclEntry.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response grantFactAccess(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact,
          @PathParam("subject") @ApiParam(value = "UUID of Subject.") @NotNull @Valid UUID subject,
          @ApiParam(hidden = true) @Valid GrantFactAccessRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Swagger won't send a request object because it's hidden from the API, thus, make sure that it's initialized.
    request = ObjectUtils.ifNull(request, new GrantFactAccessRequest());

    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.grantFactAccess(getHeader(), request.setFact(fact).setSubject(subject)))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/comments")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve a Fact's comments.",
          notes = "This operation retrieves the comments of a Fact. The request will be rejected with a 403 " +
                  "if a user does not have access to the Fact.",
          response = FactComment.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response getFactComments(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact,
          @QueryParam("before") @ApiParam(value = "Only return comments added before the given timestamp.") String before,
          @QueryParam("after") @ApiParam(value = "Only return comments added after the given timestamp.") String after
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactComments(getHeader(), new GetFactCommentsRequest()
            .setFact(fact)
            .setBefore(parseTimestamp("before", before))
            .setAfter(parseTimestamp("after", after))
    ));
  }

  @POST
  @Path("/uuid/{fact}/comments")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Add a comment to a Fact.",
          notes = "This operation adds a comment to a Fact. The request will be rejected with a 403 " +
                  "if a user does not have access to the Fact.",
          response = FactComment.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  public Response createFactComment(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact,
          @ApiParam(value = "Request to add comment.") @NotNull @Valid CreateFactCommentRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFactComment(getHeader(), request.setFact(fact)))
            .buildResponse();
  }

  private Long parseTimestamp(String parameter, String timestamp) throws InvalidArgumentException {
    try {
      return !StringUtils.isBlank(timestamp) ? Instant.parse(timestamp).toEpochMilli() : null;
    } catch (DateTimeParseException ex) {
      throw new InvalidArgumentException().addValidationError(InvalidArgumentException.ErrorMessage.PARSE, parameter, timestamp);
    }
  }

}
