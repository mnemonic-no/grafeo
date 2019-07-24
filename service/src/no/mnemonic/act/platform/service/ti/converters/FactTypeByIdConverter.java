package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class FactTypeByIdConverter implements Converter<UUID, FactType> {

  private final FactManager factManager;
  private final Function<FactTypeEntity, FactType> factTypeConverter;

  @Inject
  public FactTypeByIdConverter(FactManager factManager, Function<FactTypeEntity, FactType> factTypeConverter) {
    this.factManager = factManager;
    this.factTypeConverter = factTypeConverter;
  }

  @Override
  public Class<UUID> getSourceType() {
    return UUID.class;
  }

  @Override
  public Class<FactType> getTargetType() {
    return FactType.class;
  }

  @Override
  public FactType apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNotNull(factManager.getFactType(id), factTypeConverter, FactType.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
