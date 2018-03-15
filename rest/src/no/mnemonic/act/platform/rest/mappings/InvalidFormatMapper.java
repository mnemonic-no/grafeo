package no.mnemonic.act.platform.rest.mappings;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidFormatMapper implements ExceptionMapper<InvalidFormatException> {

  @Override
  public Response toResponse(InvalidFormatException ex) {
    String value = ObjectUtils.ifNotNull(ex.getValue(), Object::toString, "NULL");

    return ResultStash.builder()
            .setStatus(Response.Status.PRECONDITION_FAILED)
            .addFieldError("JSON field has an invalid value.", "invalid.json.field.value", MapperUtils.printPropertyPath(ex), value)
            .buildResponse();
  }

}
