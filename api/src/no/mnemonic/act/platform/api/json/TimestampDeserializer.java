package no.mnemonic.act.platform.api.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;

public class TimestampDeserializer extends JsonDeserializer<Long> {

  @Override
  public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
      return p.getLongValue();
    }
    if (p.hasToken(JsonToken.VALUE_STRING)) {
      return Instant.parse(p.getText()).toEpochMilli();
    }

    return 0L;
  }

}
