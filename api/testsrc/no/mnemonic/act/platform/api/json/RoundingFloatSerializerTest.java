package no.mnemonic.act.platform.api.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RoundingFloatSerializerTest {

  @Test
  public void testSerializeFloatRoundDown() throws IOException {
    JsonGenerator generator = mock(JsonGenerator.class);

    RoundingFloatSerializer serializer = new RoundingFloatSerializer();
    serializer.serialize(0.444f, generator, null);

    verify(generator, times(1)).writeNumber(eq(0.44f));
  }

  @Test
  public void testSerializeFloatRoundUp() throws IOException {
    JsonGenerator generator = mock(JsonGenerator.class);

    RoundingFloatSerializer serializer = new RoundingFloatSerializer();
    serializer.serialize(0.555f, generator, null);

    verify(generator, times(1)).writeNumber(eq(0.56f));
  }

}
