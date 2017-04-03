package no.mnemonic.act.platform.rest.mappings;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonProcessingMapper implements ExceptionMapper<JsonProcessingException> {

  @Override
  public Response toResponse(JsonProcessingException ex) {
    // Catch-all mapper for JSON related exceptions which aren't handled by an own mapper, e.g. invalid JSON.
    return ResultStash.builder()
            .setStatus(Response.Status.BAD_REQUEST)
            .addActionError("Invalid JSON request received.", "invalid.json.request")
            .buildResponse();
  }

}
