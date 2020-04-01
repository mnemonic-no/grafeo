package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.service.ti.converters.response.FactTypeResponseConverter;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class FactTypeByIdResponseResolver implements Function<UUID, FactType> {

  private final FactManager factManager;
  private final FactTypeResponseConverter factTypeResponseConverter;

  @Inject
  public FactTypeByIdResponseResolver(FactManager factManager, FactTypeResponseConverter factTypeResponseConverter) {
    this.factManager = factManager;
    this.factTypeResponseConverter = factTypeResponseConverter;
  }

  @Override
  public FactType apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNotNull(factManager.getFactType(id), factTypeResponseConverter, FactType.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
