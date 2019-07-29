package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Namespace;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class NamespaceByIdConverter implements Converter<UUID, Namespace> {

  @Override
  public Class<UUID> getSourceType() {
    return UUID.class;
  }

  @Override
  public Class<Namespace> getTargetType() {
    return Namespace.class;
  }

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
