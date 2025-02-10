package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.seb.model.v1.FactInfoSEB;

import jakarta.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class FactInfoDaoResolver implements Function<UUID, FactInfoSEB> {

  private final ObjectFactDao objectFactDao;
  private final FactTypeInfoDaoResolver typeResolver;

  @Inject
  public FactInfoDaoResolver(ObjectFactDao objectFactDao, FactTypeInfoDaoResolver typeResolver) {
    this.objectFactDao = objectFactDao;
    this.typeResolver = typeResolver;
  }

  @Override
  public FactInfoSEB apply(UUID id) {
    if (id == null) return null;

    FactRecord fact = objectFactDao.getFact(id);
    if (fact == null) return FactInfoSEB.builder().setId(id).build();

    return FactInfoSEB.builder()
            .setId(fact.getId())
            .setType(typeResolver.apply(fact.getTypeID()))
            .setValue(fact.getValue())
            .build();
  }
}
