package no.mnemonic.services.grafeo.utilities.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoundingFloatDeserializerTest {

  @Mock
  private JsonParser parser;
  @Mock
  private DeserializationContext context;

  private final RoundingFloatDeserializer deserializer = new RoundingFloatDeserializer();

  @Test
  public void testDeserializeInteger() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(true);
    when(parser.getFloatValue()).thenReturn(1.0f);
    assertEquals(1.0f, deserializer.deserialize(parser, context), 0.0f);
  }

  @Test
  public void testDeserializeFloatRoundDown() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(false);
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_FLOAT))).thenReturn(true);
    when(parser.getFloatValue()).thenReturn(0.444f);
    assertEquals(0.44f, deserializer.deserialize(parser, context), 0.0f);
  }

  @Test
  public void testDeserializeFloatRoundUp() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(false);
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_FLOAT))).thenReturn(true);
    when(parser.getFloatValue()).thenReturn(0.555f);
    assertEquals(0.56f, deserializer.deserialize(parser, context), 0.0f);
  }

  @Test
  public void testDeserializeInvalidToken() throws IOException {
    when(context.handleUnexpectedToken(eq(Float.class), eq(parser))).thenThrow(MismatchedInputException.class);
    assertThrows(MismatchedInputException.class, () -> deserializer.deserialize(parser, context));
  }

}
