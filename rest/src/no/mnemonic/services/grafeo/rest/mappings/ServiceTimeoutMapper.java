package no.mnemonic.services.grafeo.rest.mappings;

import no.mnemonic.services.common.api.ServiceTimeOutException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ServiceTimeoutMapper implements ExceptionMapper<ServiceTimeOutException> {

  @Override
  public Response toResponse(ServiceTimeOutException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.SERVICE_UNAVAILABLE)
            .addActionError("Request timed out, service may be overloaded or unavailable. Please try again later.", "service.timeout")
            .buildResponse();
  }

}
