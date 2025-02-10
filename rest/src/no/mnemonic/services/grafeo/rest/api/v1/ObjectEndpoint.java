package no.mnemonic.services.grafeo.rest.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectRequest;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.rest.api.ResultStash;
import no.mnemonic.services.grafeo.rest.api.auth.CredentialsResolver;

import javax.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import javax.validation.Valid;
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

@Path("/v1/object")
@Tag(name = "/v1/object")
public class ObjectEndpoint {

  private final CredentialsResolver credentialsResolver;
  private final GrafeoService service;

  @Inject
  public ObjectEndpoint(CredentialsResolver credentialsResolver, GrafeoService service) {
    this.credentialsResolver = credentialsResolver;
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve an Object by its UUID.",
          description = """
                  This operation returns an Object identified by its UUID. The result includes statistics about the
                  Facts linked to the requested Object. The request will be rejected with a 403 if a user does not have
                  access to any Facts linked to the requested Object.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Object was successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashObject.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response getObjectById(
          @PathParam("id") @Parameter(description = "UUID of the requested Object.") @NotNull @Valid UUID id,
          @QueryParam("before") @Parameter(description = "Only include Facts in statistics calculation seen before the given timestamp.") String before,
          @QueryParam("after") @Parameter(description = "Only include Facts in statistics calculation seen after the given timestamp.") String after
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
  @Operation(
          summary = "Retrieve an Object by its type and value.",
          description = """
                  This operation returns an Object identified by its ObjectType and its value. The result includes
                  statistics about the Facts linked to the requested Object. The requested type name needs to be
                  globally unique, otherwise the Object needs to be fetched by it's UUID. The request will be rejected
                  with a 403 if a user does not have access to any Facts linked to the requested Object.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Object was successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashObject.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response getObjectByTypeValue(
          @PathParam("type") @Parameter(description = "Type name of the requested Object.") @NotBlank String type,
          @PathParam("value") @Parameter(description = "Value of the requested Object.") @NotBlank String value,
          @QueryParam("before") @Parameter(description = "Only include Facts in statistics calculation seen before the given timestamp.") String before,
          @QueryParam("after") @Parameter(description = "Only include Facts in statistics calculation seen after the given timestamp.") String after
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
  @Operation(
          summary = "Retrieve Facts bound to a specific Object.",
          description = """
                  This operation returns the Facts linked to a specific Object which is identified by its UUID.
                  With the request body the user can specify which Facts will be included in the result. Only the
                  Facts a user has access to will be returned. The request will be rejected with a 403 if a user
                  does not have access to any Facts linked to the requested Object.
                  
                  This endpoint implements a similar Fact search than '/v1/fact/search' and provides mostly the same
                  search options. See the '/v1/fact/search' endpoint for more details.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Facts were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = FactEndpoint.ResultStashListFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response searchObjectFactsById(
          @PathParam("id") @Parameter(description = "UUID of Object.") @NotNull @Valid UUID id,
          @Parameter(description = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectFacts(credentialsResolver.getRequestHeader(), request.setObjectID(id)));
  }

  @POST
  @Path("/{type}/{value}/facts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Retrieve Facts bound to a specific Object.",
          description = """
                  This operation returns the Facts linked to a specific Object which is identified by its ObjectType
                  and its value. With the request body the user can specify which Facts will be included in the result.
                  Only the Facts a user has access to will be returned. The request will be rejected with a 403 if
                  a user does not have access to any Facts linked to the requested Object.
                  
                  This endpoint implements a similar Fact search than '/v1/fact/search' and provides mostly the same
                  search options. See the '/v1/fact/search' endpoint for more details.
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Facts were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = FactEndpoint.ResultStashListFact.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response searchObjectFactsByTypeValue(
          @PathParam("type") @Parameter(description = "Type name of Object.") @NotBlank String type,
          @PathParam("value") @Parameter(description = "Value of Object.") @NotBlank String value,
          @Parameter(description = "Request to limit the returned Facts.") @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjectFacts(credentialsResolver.getRequestHeader(), request.setObjectType(type).setObjectValue(value)));
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Search for Objects.",
          description = """
                  This operation searches for Objects and returns any matching Objects. The result optionally includes
                  statistics about the Facts linked to the returned Objects. With the request body the user can specify
                  which Objects will be returned. Searching against linked Facts will only be performed one level deep,
                  i.e. only Facts directly linked to an Object will be searched. The result will be restricted to the
                  Objects and Facts a user has access to. In order to return an Object a user needs to have access to
                  at least one Fact linked to the Object.
                  
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
                  possible to either specify the name or UUID of the referenced entity. With the 'minimumFactsCount' and
                  'maximumFactsCount' parameters it is possible to exclude Objects based on the number of Facts bound to
                  them. The count is calculated based on any applied Fact filters, e.g. if the search is restricted to
                  Facts of a specific FactType only those Facts will be counted. Be aware that the returned 'count' in the
                  response will be inaccurate when using 'minimumFactsCount' or 'maximumFactsCount'.
                  
                  It is allowed to request an unlimited search result (i.e. 'limit' parameter set to 0), however, the API
                  will enforce a maximum upper limit in order to protect system resources. In this case the search should
                  be narrowed down using additional search parameters.
                  
                  [0] https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html
                  """
  )
  @ApiResponses({
          @ApiResponse(
                  responseCode = "200",
                  description = "Objects were successfully retrieved.",
                  content = @Content(schema = @Schema(implementation = ResultStashListObject.class))),
          @ApiResponse(responseCode = "401", description = "User could not be authenticated."),
          @ApiResponse(responseCode = "403", description = "User is not allowed to perform this operation."),
          @ApiResponse(responseCode = "412", description = "Any parameter has an invalid format.")
  })
  @RolesAllowed("viewGrafeoFact")
  public Response searchObjects(
          @Parameter(description = "Request to search for Objects.") @NotNull @Valid SearchObjectRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjects(credentialsResolver.getRequestHeader(), request));
  }

  private Long parseTimestamp(String parameter, String timestamp) throws InvalidArgumentException {
    try {
      return !StringUtils.isBlank(timestamp) ? Instant.parse(timestamp).toEpochMilli() : null;
    } catch (DateTimeParseException ex) {
      throw new InvalidArgumentException().addValidationError(InvalidArgumentException.ErrorMessage.PARSE, parameter, timestamp);
    }
  }

  private static class ResultStashObject extends ResultStash<Object> {
  }

  private static class ResultStashListObject extends ResultStash<List<Object>> {
  }
}
