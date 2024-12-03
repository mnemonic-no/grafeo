package no.mnemonic.services.grafeo.utilities.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TimestampDeserializerTest {

  @Mock
  private JsonParser parser;
  @Mock
  private DeserializationContext context;

  private final TimestampDeserializer deserializer = new TimestampDeserializer();

  @Test
  public void testDeserializeNumber() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(true);
    when(parser.getLongValue()).thenReturn(1480520820000L);
    assertEquals(1480520820000L, (long) deserializer.deserialize(parser, context));
  }

  @Test
  public void testDeserializeString() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(false);
    when(parser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
    when(parser.getText()).thenReturn("2016-11-30T15:47:00Z");
    assertEquals(1480520820000L, (long) deserializer.deserialize(parser, context));
  }

  @Test
  public void testDeserializeStringInvalidFormat() throws IOException {
    when(context.handleWeirdStringValue(eq(Long.class), eq("invalid"), anyString())).thenThrow(InvalidFormatException.class);
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(false);
    when(parser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
    when(parser.getText()).thenReturn("invalid");
    assertThrows(InvalidFormatException.class, () -> deserializer.deserialize(parser, context));
  }

  @Test
  public void testDeserializeInvalidToken() throws IOException {
    when(context.handleUnexpectedToken(eq(Long.class), eq(parser))).thenThrow(MismatchedInputException.class);
    assertThrows(MismatchedInputException.class, () -> deserializer.deserialize(parser, context));
  }

}
