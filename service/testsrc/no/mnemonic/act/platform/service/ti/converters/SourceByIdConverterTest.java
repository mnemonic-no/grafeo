package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Source;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class SourceByIdConverterTest {

  private final SourceByIdConverter converter = new SourceByIdConverter();

  @Test
  public void testConvertSource() {
    UUID id = UUID.randomUUID();
    Source model = converter.apply(id);
    assertNotNull(model);
    assertNotNull(model.getName());
    assertEquals(id, model.getId());
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
