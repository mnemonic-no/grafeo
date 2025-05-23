package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AccessDeniedMapper implements ExceptionMapper<AccessDeniedException> {

  @Override
  public Response toResponse(AccessDeniedException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.FORBIDDEN)
            .addActionError(ex.getMessage(), "access.denied")
            .buildResponse();
  }

}
