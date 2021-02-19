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
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.act.platform.rest.api.auth.CredentialsResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;

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
import java.util.UUID;

import static no.mnemonic.act.platform.rest.api.ResultStash.buildResponse;

@Path("/v1/fact")
@Api(tags = {"experimental"})
public class FactEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final ThreatIntelligenceService service;

  @Inject
  public FactEndpoint(CredentialsResolver credentialsResolver, ThreatIntelligenceService service) {
    this.credentialsResolver = credentialsResolver;
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve a Fact by its UUID.",
          notes = "This operation returns a Fact identified by its UUID. The request will be rejected with a 403 if a " +
                  "user does not have access to the requested Fact.\n\n" +
                  "If the access mode is Public the Fact will be available to everyone. If the access mode is Explicit " +
                  "only users in the Fact's access control list will have access to the Fact. If the access mode is " +
                  "RoleBased (the default mode) a user must be either in the Fact's ACL or have general role-based " +
                  "access to the Organization owning the Fact. A user who created a Fact will always have access to it.",
          response = Fact.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Requested Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response getFactById(
          @PathParam("id") @ApiParam(value = "UUID of the requested Fact.") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFact(credentialsResolver.getRequestHeader(), new GetFactByIdRequest().setId(id)));
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Search for Facts.",
          notes = "This operation searches for Facts and returns any matching Facts. With the request body the user can " +
                  "specify which Facts will be returned. Searching against linked Objects will only be performed one " +
                  "level deep, i.e. only Objects directly linked to a Fact will be searched. Only the Facts a user has " +
                  "access to will be returned.\n\n" +
                  "Using the 'keywords' parameter in the request allows to perform a fuzzy match on the following fields: " +
                  "Fact organization, Fact origin, Fact value and Object value. The 'keywords' parameter must match one " +
                  "of those fields. The following search features are available when using this parameter.\n\n" +
                  "* A simple query string supporting the query syntax provided by Elasticsearch [0].\n" +
                  "* An IP range search, for example '1.2.3.0/24' will match all IP addresses inside the given subnet.\n" +
                  "* A domain prefix search, for example 'example.org' will also match all subdomains of 'example.org'.\n\n" +
                  "Tip: If searching by 'keywords' returns unexpected results as it might happen when an IP range search " +
                  "or domain prefix search is interpreted as a simple query string, it can be useful to filter on " +
                  "'factType' or 'objectType' in addition.\n\n" +
                  "In contrast to the fuzzy search above other filter parameters such as 'objectValue' and 'factValue' " +
                  "require an exact match. When searching on 'objectType', 'factType', 'organization' or 'origin' it is " +
                  "possible to either specify the name or UUID of the referenced entity. Retracted Facts are excluded by " +
                  "default from the search result but they are always included in the returned 'count' value (the total " +
                  "number of Facts matching the search parameters). It is allowed to request an unlimited search result " +
                  "(i.e. 'limit' parameter set to 0), however, the API will enforce a maximum upper limit in order to protect " +
                  "system resources. In this case the search should be narrowed down using additional search parameters.\n\n" +
                  "[0] https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response searchFacts(
          @ApiParam(value = "Request to search for Facts.") @NotNull @Valid SearchFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchFacts(credentialsResolver.getRequestHeader(), request));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Create a new Fact.",
          notes = "This operation creates and returns a new Fact which is bound to one or two Objects. The new Fact must " +
                  "conform to the specified FactType, i.e. the value must pass the FactType's Validator and the binding " +
                  "to Objects must respect the definition by the FactType.\n\n" +
                  "Access to the new Fact can be controlled with the 'accessMode' and 'acl' parameters in the request body. " +
                  "The 'accessMode' parameter can take one of these values:\n\n" +
                  "* Public: Fact will be publicly available to all users.\n" +
                  "* RoleBased: Fact will be accessible to users which have access to the owning organization of the Fact, " +
                  "or if explicitly added to the Fact's ACL.\n" +
                  "* Explicit: Only users given explict access to the Fact can view it, in addition to the user creating it.\n\n" +
                  "If the new Fact links to an Object which does not exist yet the missing Object will be created " +
                  "automatically with respect to the Object's ObjectType (need to pass the ObjectType's Validator).\n\n" +
                  "If a Fact with the same type, value, organization, origin, confidence, access mode and bound Objects already " +
                  "exists, no new Fact will be created. Instead the lastSeenTimestamp of the existing Fact will be updated.",
          response = Fact.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addThreatIntelFact")
  public Response createFact(
          @ApiParam(value = "Request to create Fact.") @NotNull @Valid CreateFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFact(credentialsResolver.getRequestHeader(), request))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/meta")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve a Fact's meta Facts.",
          notes = "This operation retrieves the meta Facts bound to another Fact. The request will be rejected with " +
                  "a 403 if a user does not have access to the Fact for which meta Facts should be retrieved.",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Referenced Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response getMetaFacts(
          @PathParam("fact") @ApiParam(value = "UUID of referenced Fact.") @NotNull @Valid UUID fact,
          @QueryParam("includeRetracted") @ApiParam(value = "Include retracted meta Facts (default false)") Boolean includeRetracted,
          @QueryParam("before") @ApiParam(value = "Only return meta Facts seen before a specific timestamp.") String before,
          @QueryParam("after") @ApiParam(value = "Only return meta Facts seen after a specific timestamp.") String after,
          @QueryParam("limit") @ApiParam(value = "Limit the number of returned meta Facts (default 25, 0 means all)") @Min(0) Integer limit
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.searchMetaFacts(credentialsResolver.getRequestHeader(), new SearchMetaFactsRequest()
            .setFact(fact)
            .setIncludeRetracted(includeRetracted)
            .setBefore(parseTimestamp("before", before))
            .setAfter(parseTimestamp("after", after))
            .setLimit(limit)
    ));
  }

  @POST
  @Path("/uuid/{fact}/meta")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Create a new meta Fact.",
          notes = "This operation creates and returns a new meta Fact, a Fact which is directly referencing another " +
                  "existing Fact. The new meta Fact must conform to the specified FactType, i.e. the value must pass the " +
                  "FactType's Validator and the FactType of the referenced Fact must fulfil the definition by the FactType " +
                  "of the new meta Fact.\n\n" +
                  "The access mode of a meta Fact can be more restricted than the access mode of the referenced Fact, " +
                  "but never less restricted (Public < RoleBased < Explicit). If not specified the access mode of the " +
                  "referenced Fact will be used.\n\n" +
                  "If a meta Fact with the same type, value, organization, origin, confidence and access mode exists, no " +
                  "new Fact will be created. Instead the lastSeenTimestamp of the existing Fact will be updated.\n\n" +
                  "The request will be rejected with a 403 if a user does not have access to the Fact for which the new " +
                  "meta Fact should be created.",
          response = Fact.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Referenced Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addThreatIntelFact")
  public Response createMetaFact(
          @PathParam("fact") @ApiParam(value = "UUID of referenced Fact.") @NotNull @Valid UUID fact,
          @ApiParam(value = "Request to create meta Fact.") @NotNull @Valid CreateMetaFactRequest request
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
  @ApiOperation(
          value = "Retract an existing Fact.",
          notes = "This operation retracts an already existing Fact. It creates a new meta Fact with a special 'Retraction' " +
                  "FactType which references the retracted Fact. Except of using a special FactType the new Fact is treated " +
                  "the same as every other meta Fact.\n\n" +
                  "The access mode of the retraction Fact can be more restricted than the access mode of the retracted Fact, " +
                  "but never less restricted (Public < RoleBased < Explicit). If not specified the access mode of the " +
                  "retracted Fact will be used.\n\n" +
                  "The request will be rejected with a 403 if a user does not have access to the Fact to retract.",
          response = Fact.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact to retract does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("addThreatIntelFact")
  public Response retractFact(
          @PathParam("fact") @ApiParam(value = "UUID of Fact to retract.") @NotNull @Valid UUID fact,
          @ApiParam(value = "Request to retract a Fact.") @NotNull @Valid RetractFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.retractFact(credentialsResolver.getRequestHeader(), request.setFact(fact)))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/access")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Retrieve a Fact's ACL.",
          notes = "This operation retrieves the access control list of a Fact, i.e. the list of users who were given " +
                  "explicit access to a Fact. The request will be rejected with a 403 if a user does not have access " +
                  "to the Fact.",
          response = AclEntry.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFactAccess")
  public Response getFactAcl(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactAcl(credentialsResolver.getRequestHeader(), new GetFactAclRequest().setFact(fact)));
  }

  @POST
  @Path("/uuid/{fact}/access/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Grant a Subject access to a Fact.",
          notes = "This operation grants a Subject explicit access to a non-public Fact. The request will be rejected " +
                  "with a 403 if a user does not have access to the Fact or is not allowed to grant further access.",
          response = AclEntry.class,
          code = 201
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 404, message = "Fact does not exist."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("grantThreatIntelFactAccess")
  public Response grantFactAccess(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact,
          @PathParam("subject") @ApiParam(value = "UUID or name of Subject.") @NotBlank String subject,
          @ApiParam(hidden = true) @Valid GrantFactAccessRequest request
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
  @RolesAllowed("viewThreatIntelFactComment")
  public Response getFactComments(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact,
          @QueryParam("before") @ApiParam(value = "Only return comments added before the given timestamp.") String before,
          @QueryParam("after") @ApiParam(value = "Only return comments added after the given timestamp.") String after
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
  @RolesAllowed("addThreatIntelFactComment")
  public Response createFactComment(
          @PathParam("fact") @ApiParam(value = "UUID of Fact.") @NotNull @Valid UUID fact,
          @ApiParam(value = "Request to add comment.") @NotNull @Valid CreateFactCommentRequest request
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

}
