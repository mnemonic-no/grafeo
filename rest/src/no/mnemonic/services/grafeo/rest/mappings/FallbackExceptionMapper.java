package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FallbackExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger LOGGER = Logging.getLogger(FallbackExceptionMapper.class);

  @Override
  public Response toResponse(Throwable ex) {
    LOGGER.error(ex, "An unknown exception occurred. Returning internal server error to client.");

    // Don't return anything from the exception in order to avoid information leakage.
    return ResultStash.builder()
            .setStatus(Response.Status.INTERNAL_SERVER_ERROR)
            .addActionError("An unknown server error occurred.", "unknown.server.error")
            .buildResponse();
  }

}
