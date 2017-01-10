package no.mnemonic.act.platform.rest.api.v1;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.AbstractEndpoint;
import no.mnemonic.act.platform.rest.ResultStash;
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
public class FactEndpoint extends AbstractEndpoint {

  private final ThreatIntelligenceService service;

  @Inject
  public FactEndpoint(ThreatIntelligenceService service) {
    this.service = service;
  }

  @GET
  @Path("/uuid/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFactById(
          @PathParam("id") @NotNull @Valid UUID id
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFact(getHeader(), new GetFactByIdRequest().setId(id)));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createFact(
          @NotNull @Valid CreateFactRequest request
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
  public Response retractFact(
          @PathParam("fact") @NotNull @Valid UUID fact,
          @NotNull @Valid RetractFactRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.retractFact(getHeader(), request.setFact(fact)))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/access")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFactAcl(
          @PathParam("fact") @NotNull @Valid UUID fact
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactAcl(getHeader(), new GetFactAclRequest().setFact(fact)));
  }

  @POST
  @Path("/uuid/{fact}/access/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response grantFactAccess(
          @PathParam("fact") @NotNull @Valid UUID fact,
          @PathParam("subject") @NotNull @Valid UUID subject,
          @NotNull @Valid GrantFactAccessRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.grantFactAccess(getHeader(), request.setFact(fact).setSubject(subject)))
            .buildResponse();
  }

  @GET
  @Path("/uuid/{fact}/comments")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFactComments(
          @PathParam("fact") @NotNull @Valid UUID fact,
          @QueryParam("before") String before,
          @QueryParam("after") String after
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return buildResponse(service.getFactComments(getHeader(), new GetFactCommentsRequest()
            .setFact(fact)
            .setBefore(parseTimestamp(before))
            .setAfter(parseTimestamp(after))
    ));
  }

  @POST
  @Path("/uuid/{fact}/comments")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createFactComment(
          @PathParam("fact") @NotNull @Valid UUID fact,
          @NotNull @Valid CreateFactCommentRequest request
  ) throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ResultStash.builder()
            .setStatus(Response.Status.CREATED)
            .setData(service.createFactComment(getHeader(), request.setFact(fact)))
            .buildResponse();
  }

  private Long parseTimestamp(String timestamp) throws InvalidArgumentException {
    try {
      return !StringUtils.isBlank(timestamp) ? Instant.parse(timestamp).toEpochMilli() : null;
    } catch (DateTimeParseException ex) {
      throw new InvalidArgumentException("Cannot parse timestamp: " + timestamp);
    }
  }

}
