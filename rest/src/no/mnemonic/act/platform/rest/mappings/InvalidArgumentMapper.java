package no.mnemonic.act.platform.rest.mappings;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
