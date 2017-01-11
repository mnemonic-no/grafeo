package no.mnemonic.act.platform.rest.api.v1;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.AbstractEndpoint;
import no.mnemonic.act.platform.rest.ResultStash;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/v1/factType")
public class FactTypeEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public FactTypeEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFactTypeById(
          @PathParam("id") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactType(getHeader(), new GetFactTypeByIdRequest().setId(id)));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchFactTypes()
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return buildResponse(service.searchFactTypes(getHeader(), new SearchFactTypeRequest()));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createFactType(
          @NotNull @Valid CreateFactTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFactType(getHeader(), request))
            .buildResponse();
  }

  @PUT
  @Path("/uuid/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateFactType(
          @PathParam("id") @NotNull @Valid UUID id,
          @NotNull @Valid UpdateFactTypeRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.updateFactType(getHeader(), request.setId(id)));
  }

}
