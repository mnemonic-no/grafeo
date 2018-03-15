package no.mnemonic.act.platform.api.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TimestampDeserializerTest {

  @Mock
  private JsonParser parser;
  @Mock
  private DeserializationContext context;

  private final TimestampDeserializer deserializer = new TimestampDeserializer();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testDeserializeNumber() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(true);
    when(parser.getLongValue()).thenReturn(1480520820000L);
    assertEquals(1480520820000L, (long) deserializer.deserialize(parser, context));
  }

  @Test
  public void testDeserializeString() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
    when(parser.getText()).thenReturn("2016-11-30T15:47:00Z");
    assertEquals(1480520820000L, (long) deserializer.deserialize(parser, context));
  }

  @Test(expected = InvalidFormatException.class)
  public void testDeserializeStringInvalidFormat() throws IOException {
    when(context.handleWeirdStringValue(eq(Long.class), eq("invalid"), anyString())).thenThrow(InvalidFormatException.class);
    when(parser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
    when(parser.getText()).thenReturn("invalid");
    deserializer.deserialize(parser, context);
  }

  @Test(expected = MismatchedInputException.class)
  public void testDeserializeInvalidToken() throws IOException {
    when(context.handleUnexpectedToken(eq(Long.class), eq(parser))).thenThrow(MismatchedInputException.class);
    deserializer.deserialize(parser, context);
  }

}
