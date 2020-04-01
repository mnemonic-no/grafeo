package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.Namespace;

import java.util.UUID;
import java.util.function.Function;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class NamespaceByIdResponseResolver implements Function<UUID, Namespace> {

  @Override
  public Namespace apply(UUID id) {
    if (id == null) return null;
    // For now everything will just be part of the global namespace.
    return Namespace.builder()
            .setId(GLOBAL_NAMESPACE)
            .setName("Global")
            .build();
  }
}
