package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.grafeo.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UnexpectedAuthenticationFailedMapper implements ExceptionMapper<UnexpectedAuthenticationFailedException> {

  @Override
  public Response toResponse(UnexpectedAuthenticationFailedException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.UNAUTHORIZED)
            .addActionError(ex.getMessage(), "user.not.authenticated")
            .buildResponse();
  }

}
