package no.mnemonic.act.platform.rest.mappings;

import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundMapper implements ExceptionMapper<NotFoundException> {

  @Override
  public Response toResponse(NotFoundException ex) {
    if (ex.getCause() instanceof IllegalArgumentException) {
      // If a URL parameter cannot be decoded (e.g. an invalid UUID) an IllegalArgumentException will be wrapped inside
      // a NotFoundException. But a 412 should be returned in this case.
      return ResultStash.builder()
              .setStatus(Response.Status.PRECONDITION_FAILED)
              .addActionError("Invalid URL parameter detected.", "invalid.url.parameter")
              .buildResponse();
    }

    return ResultStash.builder()
            .setStatus(Response.Status.NOT_FOUND)
            .addActionError("Requested URL does not exist.", "url.not.exist")
            .buildResponse();
  }

}
