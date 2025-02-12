package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.seb.model.v1.FactTypeInfoSEB;

import jakarta.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class FactTypeInfoDaoResolver implements Function<UUID, FactTypeInfoSEB> {

  private final FactManager factManager;

  @Inject
  public FactTypeInfoDaoResolver(FactManager factManager) {
    this.factManager = factManager;
  }

  @Override
  public FactTypeInfoSEB apply(UUID id) {
    if (id == null) return null;

    FactTypeEntity type = factManager.getFactType(id);
    if (type == null) {
      return FactTypeInfoSEB.builder()
              .setId(id)
              .setName("N/A")
              .build();
    }

    return FactTypeInfoSEB.builder()
            .setId(type.getId())
            .setName(type.getName())
            .build();
  }
}
