package no.mnemonic.act.platform.rest.api.v1;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.AbstractEndpoint;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/v1/object")
public class ObjectEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public ObjectEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getObjectById(
          @PathParam("id") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getObject(getHeader(), new GetObjectByIdRequest().setId(id)));
  }

  @GET
  @Path("/{type}/{value}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getObjectByTypeValue(
          @PathParam("type") @NotNull @Size(min = 1) String type,
          @PathParam("value") @NotNull @Size(min = 1) String value
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getObject(getHeader(), new GetObjectByTypeValueRequest().setType(type).setValue(value)));
  }

  @POST
  @Path("/uuid/{id}/facts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchObjectFactsById(
          @PathParam("id") @NotNull @Valid UUID id,
          @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.searchObjectFacts(getHeader(), request.setObjectID(id)));
  }

  @POST
  @Path("/{type}/{value}/facts")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchObjectFactsByTypeValue(
          @PathParam("type") @NotNull @Size(min = 1) String type,
          @PathParam("value") @NotNull @Size(min = 1) String value,
          @NotNull @Valid SearchObjectFactsRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.searchObjectFacts(getHeader(), request.setObjectType(type).setObjectValue(value)));
  }

  @POST
  @Path("/search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchObjects(
          @NotNull @Valid SearchObjectRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchObjects(getHeader(), request));
  }

}
