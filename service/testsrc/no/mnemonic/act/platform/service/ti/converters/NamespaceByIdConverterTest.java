package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Namespace;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;
import static org.junit.Assert.*;

public class NamespaceByIdConverterTest {

  private final NamespaceByIdConverter converter = new NamespaceByIdConverter();

  @Test
  public void testConvertNamespace() {
    Namespace model = converter.apply(UUID.randomUUID());
    assertNotNull(model);
    assertNotNull(model.getName());
    assertEquals(GLOBAL_NAMESPACE, model.getId());
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
