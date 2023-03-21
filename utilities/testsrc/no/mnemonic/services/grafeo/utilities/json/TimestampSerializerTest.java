package no.mnemonic.services.grafeo.utilities.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TimestampSerializerTest {

  @Test
  public void testSerializeTimestamp() throws IOException {
    JsonGenerator generator = mock(JsonGenerator.class);

    TimestampSerializer serializer = new TimestampSerializer();
    serializer.serialize(1480520820000L, generator, null);

    verify(generator, times(1)).writeString(eq("2016-11-30T15:47:00Z"));
  }

}
