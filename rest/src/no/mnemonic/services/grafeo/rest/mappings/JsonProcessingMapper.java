package no.mnemonic.services.grafeo.rest.mappings;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

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
