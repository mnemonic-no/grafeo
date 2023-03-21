package no.mnemonic.services.grafeo.utilities.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * {@link JsonDeserializer} which reads a float and rounds it to two decimal points.
 */
public class RoundingFloatDeserializer extends JsonDeserializer<Float> {

  private static final int DECIMAL_POINTS = 2;

  @Override
  public Float deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    // If input contains an integer just receive the float value without rounding.
    if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
      return p.getFloatValue();
    }

    // For float values round the input to two decimal points.
    if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
      return BigDecimal.valueOf(p.getFloatValue())
              .setScale(DECIMAL_POINTS, RoundingMode.HALF_UP)
              .floatValue();
    }

    // Cannot handle input, throw a MismatchedInputException.
    return (Float) ctxt.handleUnexpectedToken(Float.class, p);
  }

}
