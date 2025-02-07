package no.mnemonic.services.grafeo.rest.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.AclEntry;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.model.v1.FactComment;
import no.mnemonic.services.grafeo.api.request.v1.*;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.rest.api.ResultStash;
import no.mnemonic.services.grafeo.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultStash.buildResponse;

@Path("/v1/fact")
@Tag(name = "/v1/fact")
public class FactEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final GrafeoService service;

  @Inject
  public FactEndpoint(CredentialsResolver credentialsResolver, GrafeoService service) {
    this.credentialsResolver = credentialsResolver;
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve a Fact by its UUID.",
          description = """
                  This operation returns a Fact identified by its UUID. The request will be rejected with a 403 if a
                  user does not have access to the requested Fact.
                  
                  If the access mode is Public the Fact will be available to everyone. If the access mode is Explicit
                  only users in the Fact's access control list will have access to the Fact. If the access mode is
                  RoleBased (the default mode) a user must be either in the Fact's ACL or have general role-based
                  access to the Organization owning the Fact. A user who created a Fact will always have access to it.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Fact was successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Requested Fact does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response getFactById(
          @PathParam("id") @Parameter(description = "UUID of the requested Fact.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFact(credentialsResolver.getRequestHeader(), new GetFactByIdRequest().setId(id)));
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Search for Facts.",
          description = """
                  This operation searches for Facts and returns any matching Facts. With the request body the user can
                  specify which Facts will be returned. Searching against linked Objects will only be performed one
                  level deep, i.e. only Objects directly linked to a Fact will be searched. Only the Facts a user has
                  access to will be returned.
                  
                  Using the 'keywords' parameter in the request allows to perform a fuzzy match on the following fields:
                  Fact value and Object value. The 'keywords' parameter must match one of those fields. The following
                  search features are available when using this parameter.
                  
                  * A simple query string supporting the query syntax provided by Elasticsearch [0].
                  * An IP range search, for example '1.2.3.0/24' will match all IP addresses inside the given subnet.
                  * A domain prefix search, for example 'example.org' will also match all subdomains of 'example.org'.
                  
                  Tip: If searching by 'keywords' returns unexpected results as it might happen when an IP range search
                  or domain prefix search is interpreted as a simple query string, it can be useful to filter on
                  'factType' or 'objectType' in addition.
                  
                  In contrast to the fuzzy search above other filter parameters such as 'objectValue' and 'factValue'
                  require an exact match. When searching on 'objectType', 'factType', 'organization' or 'origin' it is
                  possible to either specify the name or UUID of the referenced entity. Retracted Facts are excluded by
                  default from the search result but they are always included in the returned 'count' description (the total
                  number of Facts matching the search parameters). It is allowed to request an unlimited search result
                  (i.e. 'limit' parameter set to 0), however, the API will enforce a maximum upper limit in order to protect
                  system resources. In this case the search should be narrowed down using additional search parameters.
                  
                  [0] https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Facts were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response searchFacts(
          @Parameter(description = "Request to search for Facts.") @NotNull @Valid SearchFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchFacts(credentialsResolver.getRequestHeader(), request));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Create a new Fact.",
          description = """
                  This operation creates and returns a new Fact which is bound to one or two Objects. The new Fact must
                  conform to the specified FactType, i.e. the value must pass the FactType's Validator and the binding
                  to Objects must respect the definition by the FactType.
                  
                  Access to the new Fact can be controlled with the 'accessMode' and 'acl' parameters in the request body.
                  The 'accessMode' parameter can take one of these values:
                  
                  * Public: Fact will be publicly available to all users.
                  * RoleBased: Fact will be accessible to users which have access to the owning organization of the Fact,
                  or if explicitly added to the Fact's ACL.
                  * Explicit: Only users given explict access to the Fact can view it, in addition to the user creating it.
                  
                  If the new Fact links to an Object which does not exist yet the missing Object will be created
                  automatically with respect to the Object's ObjectType (need to pass the ObjectType's Validator).
                  
                  If a Fact with the same type, value, organization, origin, confidence, access mode and bound Objects already
                  exists, no new Fact will be created. Instead the lastSeenTimestamp of the existing Fact will be updated.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "Fact was successfully created.",
                  content = @Content(schema = @Schema(implementation = ResultStashFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addGrafeoFact")
  public Response createFact(
          @Parameter(description = "Request to create Fact.") @NotNull @Valid CreateFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFact(credentialsResolver.getRequestHeader(), request))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/meta")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve a Fact's meta Facts.",
          description = """
                  This operation retrieves the meta Facts bound to another Fact. The request will be rejected with
                  a 403 if a user does not have access to the Fact for which meta Facts should be retrieved.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Meta Facts were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Referenced Fact does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response getMetaFacts(
          @PathParam("fact") @Parameter(description = "UUID of referenced Fact.") @NotNull @Valid UUID fact,
          @QueryParam("includeRetracted") @Parameter(description = "Include retracted meta Facts (default false)") Boolean includeRetracted,
          @QueryParam("before") @Parameter(description = "Only return meta Facts seen before a specific timestamp.") String before,
          @QueryParam("after") @Parameter(description = "Only return meta Facts seen after a specific timestamp.") String after,
          @QueryParam("limit") @Parameter(description = "Limit the number of returned meta Facts (default 25, 0 means all)") @Min(0) Integer limit
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.searchMetaFacts(credentialsResolver.getRequestHeader(), new SearchMetaFactsRequest()
            .setFact(fact)
            .setIncludeRetracted(includeRetracted)
            .setEndTimestamp(parseTimestamp("before", before))
            .setStartTimestamp(parseTimestamp("after", after))
            .setLimit(limit)
    ));
  }

  @POST
  @Path("/uuid/{fact}/meta")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Create a new meta Fact.",
          description = """
                  This operation creates and returns a new meta Fact, a Fact which is directly referencing another
                  existing Fact. The new meta Fact must conform to the specified FactType, i.e. the value must pass the
                  FactType's Validator and the FactType of the referenced Fact must fulfil the definition by the FactType
                  of the new meta Fact.
                  
                  The access mode of a meta Fact can be more restricted than the access mode of the referenced Fact,
                  but never less restricted (Public < RoleBased < Explicit). If not specified the access mode of the
                  referenced Fact will be used.
                  
                  If a meta Fact with the same type, value, organization, origin, confidence and access mode exists,
                  no new Fact will be created. Instead the lastSeenTimestamp of the existing Fact will be updated.
                  
                  The request will be rejected with a 403 if a user does not have access to the Fact for which the new
                  meta Fact should be created.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "Meta Fact was successfully created.",
                  content = @Content(schema = @Schema(implementation = ResultStashFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Referenced Fact does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addGrafeoFact")
  public Response createMetaFact(
          @PathParam("fact") @Parameter(description = "UUID of referenced Fact.") @NotNull @Valid UUID fact,
          @Parameter(description = "Request to create meta Fact.") @NotNull @Valid CreateMetaFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createMetaFact(credentialsResolver.getRequestHeader(), request.setFact(fact)))
            .buildResponse();
  }

  @POST
  @Path("/uuid/{fact}/retract")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retract an existing Fact.",
          description = """
                  This operation retracts an already existing Fact. It creates a new meta Fact with a special 'Retraction'
                  FactType which references the retracted Fact. Except of using a special FactType the new Fact is treated
                  the same as every other meta Fact.
                  
                  The access mode of the retraction Fact can be more restricted than the access mode of the retracted Fact,
                  but never less restricted (Public < RoleBased < Explicit). If not specified the access mode of the
                  retracted Fact will be used.
                  
                  The request will be rejected with a 403 if a user does not have access to the Fact to retract.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "Fact was successfully retracted.",
                  content = @Content(schema = @Schema(implementation = ResultStashFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Fact to retract does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addGrafeoFact")
  public Response retractFact(
          @PathParam("fact") @Parameter(description = "UUID of Fact to retract.") @NotNull @Valid UUID fact,
          @Parameter(description = "Request to retract a Fact.") @NotNull @Valid RetractFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.retractFact(credentialsResolver.getRequestHeader(), request.setFact(fact)))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/access")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve a Fact's ACL.",
          description = """
                  This operation retrieves the access control list of a Fact, i.e. the list of users who were given
                  explicit access to a Fact. The request will be rejected with a 403 if a user does not have access
                  to the Fact.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "ACL was successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListAclEntry.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Fact does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFactAccess")
  public Response getFactAcl(
          @PathParam("fact") @Parameter(description = "UUID of Fact.") @NotNull @Valid UUID fact
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactAcl(credentialsResolver.getRequestHeader(), new GetFactAclRequest().setFact(fact)));
  }

  @POST
  @Path("/uuid/{fact}/access/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Grant a Subject access to a Fact.",
          description = """
                  This operation grants a Subject explicit access to a non-public Fact. The request will be rejected
                  with a 403 if a user does not have access to the Fact or is not allowed to grant further access.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "Access was successfully granted.",
                  content = @Content(schema = @Schema(implementation = ResultStashAclEntry.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Fact does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("grantGrafeoFactAccess")
  public Response grantFactAccess(
          @PathParam("fact") @Parameter(description = "UUID of Fact.") @NotNull @Valid UUID fact,
          @PathParam("subject") @Parameter(description = "UUID or name of Subject.") @NotBlank String subject,
          @Parameter(hidden = true) @Valid GrantFactAccessRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Swagger won't send a request object because it's hidden from the API, thus, make sure that it's initialized.
    request = ObjectUtils.ifNull(request, new GrantFactAccessRequest());

    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.grantFactAccess(credentialsResolver.getRequestHeader(), request.setFact(fact).setSubject(subject)))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/comments")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve a Fact's comments.",
          description = """
                  This operation retrieves the comments of a Fact. The request will be rejected with a 403
                  if a user does not have access to the Fact.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Comments were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListFactComment.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Fact does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFactComment")
  public Response getFactComments(
          @PathParam("fact") @Parameter(description = "UUID of Fact.") @NotNull @Valid UUID fact,
          @QueryParam("before") @Parameter(description = "Only return comments added before the given timestamp.") String before,
          @QueryParam("after") @Parameter(description = "Only return comments added after the given timestamp.") String after
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactComments(credentialsResolver.getRequestHeader(), new GetFactCommentsRequest()
            .setFact(fact)
            .setBefore(parseTimestamp("before", before))
            .setAfter(parseTimestamp("after", after))
    ));
  }

  @POST
  @Path("/uuid/{fact}/comments")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Add a comment to a Fact.",
          description = """
                  This operation adds a comment to a Fact. The request will be rejected with a 403
                  if a user does not have access to the Fact.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "201",
                  description = "Commented was successfully added.",
                  content = @Content(schema = @Schema(implementation = ResultStashFactComment.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "404", description = "Fact does not exist."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addGrafeoFactComment")
  public Response createFactComment(
          @PathParam("fact") @Parameter(description = "UUID of Fact.") @NotNull @Valid UUID fact,
          @Parameter(description = "Request to add comment.") @NotNull @Valid CreateFactCommentRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFactComment(credentialsResolver.getRequestHeader(), request.setFact(fact)))
            .buildResponse();
  }

  private Long parseTimestamp(String parameter, String timestamp) throws InvalidArgumentException {
    try {
      return !StringUtils.isBlank(timestamp) ? Instant.parse(timestamp).toEpochMilli() : null;
    } catch (DateTimeParseException ex) {
      throw new InvalidArgumentException().addValidationError(InvalidArgumentException.ErrorMessage.PARSE, parameter, timestamp);
    }
  }

  private static class ResultStashAclEntry extends ResultStash<AclEntry> {
  }

  private static class ResultStashListAclEntry extends ResultStash<List<AclEntry>> {
  }

  static class ResultStashFact extends ResultStash<Fact> {
  }

  static class ResultStashListFact extends ResultStash<List<Fact>> {
  }

  private static class ResultStashFactComment extends ResultStash<FactComment> {
  }

  private static class ResultStashListFactComment extends ResultStash<List<FactComment>> {
  }
}
