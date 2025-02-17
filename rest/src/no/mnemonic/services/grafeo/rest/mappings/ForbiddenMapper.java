package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

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
