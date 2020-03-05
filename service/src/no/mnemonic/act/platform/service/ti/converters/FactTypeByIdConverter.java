package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class FactTypeByIdConverter implements Function<UUID, FactType> {

  private final FactManager factManager;
  private final FactTypeConverter factTypeConverter;

  @Inject
  public FactTypeByIdConverter(FactManager factManager, FactTypeConverter factTypeConverter) {
    this.factManager = factManager;
    this.factTypeConverter = factTypeConverter;
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
