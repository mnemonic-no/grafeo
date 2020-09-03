package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.seb.model.v1.OriginInfoSEB;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OriginInfoResolver implements Function<UUID, OriginInfoSEB> {

  private final OriginManager originManager;

  @Inject
  public OriginInfoResolver(OriginManager originManager) {
    this.originManager = originManager;
  }

  @Override
  public OriginInfoSEB apply(UUID id) {
    if (id == null) return null;

    OriginEntity origin = originManager.getOrigin(id);
    if (origin == null) return null;

    return OriginInfoSEB.builder()
            .setId(origin.getId())
            .setName(origin.getName())
            .build();
  }
}
