package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidArgumentMapper implements ExceptionMapper<InvalidArgumentException> {

  @Override
  public Response toResponse(InvalidArgumentException ex) {
    ResultStash.Builder builder = ResultStash.builder().setStatus(Response.Status.PRECONDITION_FAILED);
    ex.getValidationErrors().forEach(error -> builder.addFieldError(error.getMessage(), error.getMessageTemplate(),
            error.getProperty(), error.getValue()));
    return builder.buildResponse();
  }

}
