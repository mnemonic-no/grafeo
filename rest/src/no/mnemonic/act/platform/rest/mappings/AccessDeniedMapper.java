package no.mnemonic.act.platform.rest.mappings;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
