package no.mnemonic.services.grafeo.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.OperationTimeoutException;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphByObjectSearchRequest;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphRequest;
import no.mnemonic.services.grafeo.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.services.grafeo.rest.api.ResultStash;
import no.mnemonic.services.grafeo.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultStash.buildResponse;

@Path("v1/traverse")
@Api(tags = {"development"})
public class TraverseEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final ThreatIntelligenceService service;

  @Inject
  public TraverseEndpoint(CredentialsResolver credentialsResolver, ThreatIntelligenceService service) {
    this.credentialsResolver = credentialsResolver;
    this.service = service;
  }

  @POST
  @Path("/object/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at a specific Object.",
          notes = "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal. " +
                  "Objects are represented as graph vertices and Facts as graph edges. The labels of vertices and edges " +
                  "are the names of the corresponding ObjectTypes and FactTypes, respectively.\n\n" +
                  "The traversal query contained in the request must be a valid Gremlin query [0,1]. Inside the query the " +
                  "graph is referenced as 'g' and the starting point of the traversal is set to the Object specified in the " +
                  "request. Therefore, it is not necessary to either instantiate a graph instance or to set the starting " +
                  "point of the traversal by using V() or E(). For example, a query to fetch all outgoing edges from the " +
                  "starting Object would simply be 'g.outE()'.\n\n" +
                  "There are several request parameters that affect the graph traversal as a whole. " +
                  "The 'limit' controls the maximum number of elements in the result. The default is 25. " +
                  "The 'includeRetracted' parameter controls wether retracted fact edges are followed during traversal. " +
                  "The 'before' and 'after' parameters filter facts by timestamp.\n\n" +
                  "Object properties include any one-legged facts associated with the object as well as the Object " +
                  "'value'. Fact properties include any meta-facts associated with the fact. They can be accessed " +
                  "by using a 'meta/' prefix, e.g 'has('meta/tlp')'. " +
                  "In addition, the following properties are available: " +
                  "'accessMode', 'addedByID', 'addedByName', 'isRetracted', 'lastSeenTimestamp' 'organizationID', " +
                  "'organizationName', 'originID', 'originName', 'timestamp', 'trust', 'certainty', 'confidence' " +
                  "and 'value'.\n\n" +
                  "Permissions are checked during traversal, which means that only edges the user has access to will be " +
                  "followed. Only properties the user has access to will be available.\n\n" +
                  "If the result of the graph traversal are edges the response will contain the Facts belonging to those " +
                  "edges. If the result are vertices the response will contain Objects. In all other cases the result " +
                  "of the traversal is returned as-is, for instance, when the result is a list of vertex or edge " +
                  "properties.\n\n" +
                  "[0] Tutorial: https://tinkerpop.apache.org/docs/current/tutorials/getting-started/\n\n" +
                  "[1] Reference documentation: https://tinkerpop.apache.org/docs/current/reference/",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 408, message = "Execution of this operation timed out."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("traverseThreatIntelFact")
  public Response traverseByObjectId(
          @PathParam("id") @ApiParam(value = "UUID of Object.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseGraphRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverse(
            credentialsResolver.getRequestHeader(),
            TraverseGraphByObjectsRequest.from(request, id.toString())));
  }

  @POST
  @Path("/object/{type}/{value}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at a specific Object.",
          notes = "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal. " +
                  "For more information about traversal, see '/v1/traverse/object/{id}'.",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 408, message = "Execution of this operation timed out."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("traverseThreatIntelFact")
  public Response traverseByObjectTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of Object.") @NotBlank String value,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseGraphRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverse(
            credentialsResolver.getRequestHeader(),
            TraverseGraphByObjectsRequest.from(request, type + "/" + value)));
  }

  @POST
  @Path("/objects")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at a list of Objects.",
          notes = "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal. " +
                  "The set of starting objects may be identified by either object id or object type and value, e.g " +
                  "'threatActor/Sofacy'." +
                  "For more information about traversal, see '/v1/traverse/object/{id}'.",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 408, message = "Execution of this operation timed out."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("traverseThreatIntelFact")
  public Response traverseByObjects(
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseGraphByObjectsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverse(credentialsResolver.getRequestHeader(), request));
  }

  @POST
  @Path("/objects/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph after performing an Object search.",
          notes = "This operation first performs an Object search and then traverses the graph of Objects and Facts " +
                  "starting at the Objects returned from the Object search. " +
                  "Note that the Object search and the traversal have designated parameters. " +
                  "The search parameters only affect the number of Objects from which the traversal will start from. " +
                  "The traversal parameters only affect how the ensuing traversal is performed. " +
                  "For example, setting a search limit is independent of the traversal limit. " +
                  "The Object search accepts the same parameters as '/v1/object/search'. " +
                  "The traversal accepts the same parameters as '/v1/traverse/object/{id}'. " +
                  "See those endpoints for further details.\n\n",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 408, message = "Execution of this operation timed out."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("traverseThreatIntelFact")
  public Response traverseByObjectSearch(
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseGraphByObjectSearchRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverse(credentialsResolver.getRequestHeader(), request));
  }
}
