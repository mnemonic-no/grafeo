package no.mnemonic.act.platform.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectTypeValueRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.act.platform.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static no.mnemonic.act.platform.rest.api.ResultStash.buildResponse;

@Path("v1/traverse")
@Api(tags = {"experimental"})
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
                  "If the result of the graph traversal are edges the response will contain the Facts belonging to those " +
                  "edges. If the result are vertices the response will contain Objects. In all other cases the result " +
                  "of the traversal is returned as-is, for instance, when the result is a list of vertex or edge properties.\n\n" +
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
  @RolesAllowed("traverseFactObjects")
  public Response traversebyObjectId(
          @PathParam("id") @ApiParam(value = "UUID of Object.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseGraphByObjectIdRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverse(credentialsResolver.getRequestHeader(), request.setId(id)));
  }

  @POST
  @Path("/object/{type}/{value}")
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
                  "If the result of the graph traversal are edges the response will contain the Facts belonging to those " +
                  "edges. If the result are vertices the response will contain Objects. In all other cases the result " +
                  "of the traversal is returned as-is, for instance, when the result is a list of vertex or edge properties.\n\n" +
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
  @RolesAllowed("traverseFactObjects")
  public Response traverseByObjectTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of Object.") @NotBlank String value,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseGraphByObjectTypeValueRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverse(credentialsResolver.getRequestHeader(), request.setType(type).setValue(value)));
  }

  @POST
  @Path("/objects")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at a list of Objects.",
          notes = "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal." +
                  "The set of starting objects may be identified by either object id or object type and value." +
                  "For more information about traversal, see '/v1/traverse/object/{type}/{value}'.",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 408, message = "Execution of this operation timed out."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("traverseFactObjects")
  public Response traverseByObjects(
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseGraphByObjectsRequest request
          ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverse(credentialsResolver.getRequestHeader(), request));
  }
}
