package no.mnemonic.services.grafeo.rest.api.v1;

import io.swagger.annotations.*;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.OperationTimeoutException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.request.v1.*;
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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static no.mnemonic.services.grafeo.rest.api.ResultStash.buildResponse;

@Path("/v1/object")
@Api(tags = {"development"})
public class ObjectEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final ThreatIntelligenceService service;

  @Inject
  public ObjectEndpoint(CredentialsResolver credentialsResolver, ThreatIntelligenceService service) {
    this.credentialsResolver = credentialsResolver;
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
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response getObjectById(
          @PathParam("id") @ApiParam(value = "UUID of the requested Object.") @NotNull @Valid UUID id,
          @QueryParam("before") @ApiParam(value = "Only include Facts in statistics calculation seen before the given timestamp.") String before,
          @QueryParam("after") @ApiParam(value = "Only include Facts in statistics calculation seen after the given timestamp.") String after
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.getObject(credentialsResolver.getRequestHeader(), new GetObjectByIdRequest()
            .setId(id)
            .setBefore(parseTimestamp("before", before))
            .setAfter(parseTimestamp("after", after))
    ));
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
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response getObjectByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of the requested Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of the requested Object.") @NotBlank String value,
          @QueryParam("before") @ApiParam(value = "Only include Facts in statistics calculation seen before the given timestamp.") String before,
          @QueryParam("after") @ApiParam(value = "Only include Facts in statistics calculation seen after the given timestamp.") String after
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.getObject(credentialsResolver.getRequestHeader(), new GetObjectByTypeValueRequest()
            .setType(type)
            .setValue(value)
            .setBefore(parseTimestamp("before", before))
            .setAfter(parseTimestamp("after", after))
    ));
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
                  "does not have access to any Facts linked to the requested Object.\n\n" +
                  "This endpoint implements a similar Fact search than '/v1/fact/search' and provides mostly the same " +
                  "search options. See the '/v1/fact/search' endpoint for more details.",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response searchObjectFactsById(
          @PathParam("id") @ApiParam(value = "UUID of Object.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectFacts(credentialsResolver.getRequestHeader(), request.setObjectID(id)));
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
                  "a user does not have access to any Facts linked to the requested Object.\n\n" +
                  "This endpoint implements a similar Fact search than '/v1/fact/search' and provides mostly the same " +
                  "search options. See the '/v1/fact/search' endpoint for more details.",
          response = Fact.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response searchObjectFactsByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of Object.") @NotBlank String value,
          @ApiParam(value = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectFacts(credentialsResolver.getRequestHeader(), request.setObjectType(type).setObjectValue(value)));
  }

  @Deprecated
  @POST
  @Path("/uuid/{id}/traverse")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at a specific Object.",
          notes = "Deprecated, use 'v1/traverse/object/{id}' instead.\n\n" +
                  "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal. " +
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
  @RolesAllowed("traverseThreatIntelFact")
  public Response traverseObjectById(
          @PathParam("id") @ApiParam(value = "UUID of Object.") @NotNull @Valid UUID id,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseByObjectIdRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverseGraph(credentialsResolver.getRequestHeader(), request.setId(id)));
  }

  @Deprecated
  @POST
  @Path("/{type}/{value}/traverse")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph starting at a specific Object.",
          notes = "Deprecated, use 'v1/traverse/object/{type}/{value}' instead.\n\n" +
                  "This operation traverses the graph of Objects and Facts, and returns the result of the graph traversal. " +
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
  @RolesAllowed("traverseThreatIntelFact")
  public Response traverseObjectByTypeValue(
          @PathParam("type") @ApiParam(value = "Type name of Object.") @NotBlank String type,
          @PathParam("value") @ApiParam(value = "Value of Object.") @NotBlank String value,
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseByObjectTypeValueRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverseGraph(credentialsResolver.getRequestHeader(), request.setType(type).setValue(value)));
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Search for Objects.",
          notes = "This operation searches for Objects and returns any matching Objects. The result optionally includes " +
                  "statistics about the Facts linked to the returned Objects. With the request body the user can specify " +
                  "which Objects will be returned. Searching against linked Facts will only be performed one level deep, " +
                  "i.e. only Facts directly linked to an Object will be searched. The result will be restricted to the " +
                  "Objects and Facts a user has access to. In order to return an Object a user needs to have access to " +
                  "at least one Fact linked to the Object.\n\n" +
                  "Using the 'keywords' parameter in the request allows to perform a fuzzy match on the following fields: " +
                  "Fact value and Object value. The 'keywords' parameter must match one of those fields. The following " +
                  "search features are available when using this parameter.\n\n" +
                  "* A simple query string supporting the query syntax provided by Elasticsearch [0].\n" +
                  "* An IP range search, for example '1.2.3.0/24' will match all IP addresses inside the given subnet.\n" +
                  "* A domain prefix search, for example 'example.org' will also match all subdomains of 'example.org'.\n\n" +
                  "Tip: If searching by 'keywords' returns unexpected results as it might happen when an IP range search " +
                  "or domain prefix search is interpreted as a simple query string, it can be useful to filter on " +
                  "'factType' or 'objectType' in addition.\n\n" +
                  "In contrast to the fuzzy search above other filter parameters such as 'objectValue' and 'factValue' " +
                  "require an exact match. When searching on 'objectType', 'factType', 'organization' or 'origin' it is " +
                  "possible to either specify the name or UUID of the referenced entity. With the 'minimumFactsCount' and " +
                  "'maximumFactsCount' parameters it is possible to exclude Objects based on the number of Facts bound to " +
                  "them. The count is calculated based on any applied Fact filters, e.g. if the search is restricted to " +
                  "Facts of a specific FactType only those Facts will be counted. Be aware that the returned 'count' in the " +
                  "response will be inaccurate when using 'minimumFactsCount' or 'maximumFactsCount'.\n\n" +
                  "It is allowed to request an unlimited search result (i.e. 'limit' parameter set to 0), however, the API " +
                  "will enforce a maximum upper limit in order to protect system resources. In this case the search should " +
                  "be narrowed down using additional search parameters.\n\n" +
                  "[0] https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html",
          response = Object.class,
          responseContainer = "list"
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewThreatIntelFact")
  public Response searchObjects(
          @ApiParam(value = "Request to search for Objects.") @NotNull @Valid SearchObjectRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjects(credentialsResolver.getRequestHeader(), request));
  }

  @Deprecated
  @POST
  @Path("/traverse")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
          value = "Traverse the Object/Fact graph after performing an Object search.",
          notes = "Deprecated, use 'v1/traverse/object/search' instead.\n\n" +
                  "This operation performs first an Object search and then traverses the graph of Objects and Facts " +
                  "starting at the Objects returned from the Object search. For more information about Object search " +
                  "see '/v1/object/search' and about graph traversal '/v1/object/{type}/{value}/traverse'. This operation " +
                  "accepts the same search parameters than '/v1/object/search' in addition to a Gremlin query for the " +
                  "graph traversal. Note that any limit provided in the request will only be applied to the Object search " +
                  "and not to the graph traversal. A limit to the graph traversal must be provided as part of the Gremlin query.",
          response = ResultStash.class
  )
  @ApiResponses({
          @ApiResponse(code = 401, message = "User could not be authenticated."),
          @ApiResponse(code = 403, message = "User is not allowed to perform this operation."),
          @ApiResponse(code = 408, message = "Execution of this operation timed out."),
          @ApiResponse(code = 412, message = "Any parameter has an invalid format.")
  })
  @RolesAllowed("traverseThreatIntelFact")
  public Response traverseObjects(
          @ApiParam(value = "Request to traverse graph.") @NotNull @Valid TraverseByObjectSearchRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    return buildResponse(service.traverseGraph(credentialsResolver.getRequestHeader(), request));
  }

  private Long parseTimestamp(String parameter, String timestamp) throws InvalidArgumentException {
    try {
      return !StringUtils.isBlank(timestamp) ? Instant.parse(timestamp).toEpochMilli() : null;
    } catch (DateTimeParseException ex) {
      throw new InvalidArgumentException().addValidationError(InvalidArgumentException.ErrorMessage.PARSE, parameter, timestamp);
    }
  }

}
