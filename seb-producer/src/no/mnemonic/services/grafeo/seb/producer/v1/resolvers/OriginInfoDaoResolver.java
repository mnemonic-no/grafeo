package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.seb.model.v1.OriginInfoSEB;

import jakarta.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OriginInfoDaoResolver implements Function<UUID, OriginInfoSEB> {

  private final OriginManager originManager;

  @Inject
  public OriginInfoDaoResolver(OriginManager originManager) {
    this.originManager = originManager;
  }

  @Override
  public OriginInfoSEB apply(UUID id) {
    if (id == null) return null;

    OriginEntity origin = originManager.getOrigin(id);
    if (origin == null) {
      return OriginInfoSEB.builder()
              .setId(id)
              .setName("N/A")
              .build();
    }

    return OriginInfoSEB.builder()
            .setId(origin.getId())
            .setName(origin.getName())
            .build();
  }
}
