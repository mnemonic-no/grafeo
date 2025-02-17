package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AuthenticationFailedMapper implements ExceptionMapper<AuthenticationFailedException> {

  @Override
  public Response toResponse(AuthenticationFailedException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.UNAUTHORIZED)
            .addActionError(ex.getMessage(), "user.not.authenticated")
            .buildResponse();
  }

}
