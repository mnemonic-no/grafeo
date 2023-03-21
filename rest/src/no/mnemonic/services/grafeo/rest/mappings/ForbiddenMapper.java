package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.grafeo.rest.api.ResultStash;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ForbiddenMapper implements ExceptionMapper<ForbiddenException> {

  @Override
  public Response toResponse(ForbiddenException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.FORBIDDEN)
            .addActionError("Access denied to requested URL.", "access.denied")
            .buildResponse();
  }

}
