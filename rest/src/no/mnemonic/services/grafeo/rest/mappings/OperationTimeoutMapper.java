package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.grafeo.api.exceptions.OperationTimeoutException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OperationTimeoutMapper implements ExceptionMapper<OperationTimeoutException> {

  @Override
  public Response toResponse(OperationTimeoutException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.REQUEST_TIMEOUT)
            .addActionError(ex.getMessage(), ex.getMessageTemplate())
            .buildResponse();
  }

}
