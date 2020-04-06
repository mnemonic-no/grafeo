package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.service.ti.converters.response.FactTypeResponseConverter;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class FactTypeByIdResponseResolver implements Function<UUID, FactType> {

  private final FactManager factManager;
  private final FactTypeResponseConverter factTypeResponseConverter;
  private final Map<UUID, FactType> responseCache;

  @Inject
  public FactTypeByIdResponseResolver(FactManager factManager,
                                      FactTypeResponseConverter factTypeResponseConverter,
                                      Map<UUID, FactType> responseCache) {
    this.factManager = factManager;
    this.factTypeResponseConverter = factTypeResponseConverter;
    this.responseCache = responseCache;
  }

  @Override
  public FactType apply(UUID id) {
    if (id == null) return null;
    return responseCache.computeIfAbsent(id, this::resolveUncached);
  }

  private FactType resolveUncached(UUID id) {
    return ObjectUtils.ifNotNull(factManager.getFactType(id), factTypeResponseConverter, FactType.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
