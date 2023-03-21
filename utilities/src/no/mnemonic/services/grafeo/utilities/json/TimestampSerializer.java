package no.mnemonic.services.grafeo.utilities.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;

/**
 * {@link JsonSerializer} which writes a timestamp as ISO8601, e.g. "2016-09-28T21:26:22Z".
 */
public class TimestampSerializer extends JsonSerializer<Long> {

  @Override
  public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(Instant.ofEpochMilli(value).toString());
  }

}
