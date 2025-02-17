package no.mnemonic.services.grafeo.rest.mappings;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnrecognizedPropertyMapper implements ExceptionMapper<UnrecognizedPropertyException> {

  @Override
  public Response toResponse(UnrecognizedPropertyException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.PRECONDITION_FAILED)
            .addFieldError("Unknown JSON field detected.", "unknown.json.field", MapperUtils.printPropertyPath(ex), "")
            .buildResponse();
  }

}
