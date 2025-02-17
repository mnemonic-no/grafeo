package no.mnemonic.services.grafeo.rest.mappings;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MismatchedInputMapper implements ExceptionMapper<MismatchedInputException> {

  @Override
  public Response toResponse(MismatchedInputException ex) {
    return ResultStash.builder()
            .setStatus(Response.Status.PRECONDITION_FAILED)
            .addFieldError("JSON field has an invalid type.", "invalid.json.field.type", MapperUtils.printPropertyPath(ex), "")
            .buildResponse();
  }

}
