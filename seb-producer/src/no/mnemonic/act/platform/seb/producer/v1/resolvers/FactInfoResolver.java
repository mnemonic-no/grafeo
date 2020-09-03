package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.seb.model.v1.FactInfoSEB;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class FactInfoResolver implements Function<UUID, FactInfoSEB> {

  private final FactManager factManager;
  private final FactTypeInfoResolver typeResolver;

  @Inject
  public FactInfoResolver(FactManager factManager, FactTypeInfoResolver typeResolver) {
    this.factManager = factManager;
    this.typeResolver = typeResolver;
  }

  @Override
  public FactInfoSEB apply(UUID id) {
    if (id == null) return null;

    // Use FactManager directly instead of ObjectFactDao to avoid resolving related information
    // because only the basic information (id, type, value) is required here.
    FactEntity fact = factManager.getFact(id);
    if (fact == null) return null;

    return FactInfoSEB.builder()
            .setId(fact.getId())
            .setType(typeResolver.apply(fact.getTypeID()))
            .setValue(fact.getValue())
            .build();
  }
}
