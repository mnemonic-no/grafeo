package no.mnemonic.services.grafeo.service.ti.resolvers.response;

import no.mnemonic.services.grafeo.api.model.v1.Namespace;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.services.grafeo.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;
import static org.junit.Assert.*;

public class NamespaceByIdResponseResolverTest {

  private final NamespaceByIdResponseResolver converter = new NamespaceByIdResponseResolver();

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
