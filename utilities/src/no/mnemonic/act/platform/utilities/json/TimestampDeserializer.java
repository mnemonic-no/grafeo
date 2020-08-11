package no.mnemonic.act.platform.utilities.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * {@link JsonDeserializer} which reads a timestamp using ISO8601, e.g. "2016-09-28T21:26:22Z",
 * and converts it to a long (i.e. milliseconds since start of UNIX epoch).
 */
public class TimestampDeserializer extends JsonDeserializer<Long> {

  @Override
  public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    // Input is already a long, just return value directly.
    if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
      return p.getLongValue();
    }

    // Try to convert input to an Instant, else throw an InvalidFormatException.
    if (p.hasToken(JsonToken.VALUE_STRING)) {
      try {
        return Instant.parse(p.getText()).toEpochMilli();
      } catch (DateTimeParseException ignored) {
        return (Long) ctxt.handleWeirdStringValue(Long.class, p.getText(), "Cannot convert to valid Instant timestamp");
      }
    }

    // Cannot handle input, throw a MismatchedInputException.
    return (Long) ctxt.handleUnexpectedToken(Long.class, p);
  }

}
