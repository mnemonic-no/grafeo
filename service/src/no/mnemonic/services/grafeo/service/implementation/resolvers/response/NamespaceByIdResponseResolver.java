package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.services.grafeo.api.model.v1.Namespace;

import java.util.UUID;
import java.util.function.Function;

import static no.mnemonic.services.grafeo.service.implementation.GrafeoServiceImpl.GLOBAL_NAMESPACE;

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
