package no.mnemonic.services.grafeo.utilities.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * {@link JsonSerializer} which writes a float rounded to two decimal points.
 */
public class RoundingFloatSerializer extends JsonSerializer<Float> {

  private static final int DECIMAL_POINTS = 2;

  @Override
  public void serialize(Float value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeNumber(BigDecimal.valueOf(value)
            .setScale(DECIMAL_POINTS, RoundingMode.HALF_UP)
            .floatValue()
    );
  }

}
