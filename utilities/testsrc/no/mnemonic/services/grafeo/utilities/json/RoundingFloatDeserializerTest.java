package no.mnemonic.services.grafeo.utilities.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RoundingFloatDeserializerTest {

  @Mock
  private JsonParser parser;
  @Mock
  private DeserializationContext context;

  private final RoundingFloatDeserializer deserializer = new RoundingFloatDeserializer();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testDeserializeInteger() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(true);
    when(parser.getFloatValue()).thenReturn(1.0f);
    assertEquals(1.0f, deserializer.deserialize(parser, context), 0.0f);
  }

  @Test
  public void testDeserializeFloatRoundDown() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_FLOAT))).thenReturn(true);
    when(parser.getFloatValue()).thenReturn(0.444f);
    assertEquals(0.44f, deserializer.deserialize(parser, context), 0.0f);
  }

  @Test
  public void testDeserializeFloatRoundUp() throws IOException {
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_FLOAT))).thenReturn(true);
    when(parser.getFloatValue()).thenReturn(0.555f);
    assertEquals(0.56f, deserializer.deserialize(parser, context), 0.0f);
  }

  @Test(expected = MismatchedInputException.class)
  public void testDeserializeInvalidToken() throws IOException {
    when(context.handleUnexpectedToken(eq(Float.class), eq(parser))).thenThrow(MismatchedInputException.class);
    deserializer.deserialize(parser, context);
  }

}
