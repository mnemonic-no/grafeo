package no.mnemonic.act.platform.api.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimestampDeserializerTest {

  @Test
  public void testDeserializeNumber() throws IOException {
    JsonParser parser = mock(JsonParser.class);
    when(parser.hasToken(eq(JsonToken.VALUE_NUMBER_INT))).thenReturn(true);
    when(parser.getLongValue()).thenReturn(1480520820000L);

    TimestampDeserializer deserializer = new TimestampDeserializer();
    long timestamp = deserializer.deserialize(parser, null);

    assertEquals(1480520820000L, timestamp);
  }

  @Test
  public void testDeserializeString() throws IOException {
    JsonParser parser = mock(JsonParser.class);
    when(parser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
    when(parser.getText()).thenReturn("2016-11-30T15:47:00Z");

    TimestampDeserializer deserializer = new TimestampDeserializer();
    long timestamp = deserializer.deserialize(parser, null);

    assertEquals(1480520820000L, timestamp);
  }

}
