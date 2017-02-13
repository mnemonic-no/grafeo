package no.mnemonic.act.platform.rest.mappings;

import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Date;

@Provider
public class FallbackExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable ex) {
    // For now just dump everything to the console, until we have done something about logging.
    System.err.println(new Date().toString() + " - An unknown exception occurred:");
    ex.printStackTrace();

    // Don't return anything from the exception in order to avoid information leakage.
    return ResultStash.builder()
            .setStatus(Response.Status.INTERNAL_SERVER_ERROR)
            .addActionError("An unknown server error occurred.", "unknown.server.error")
            .buildResponse();
  }

}
