package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.OriginResolver;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class OriginByIdResponseResolver implements Function<UUID, Origin> {

  private final OriginResolver originResolver;
  private final OriginResponseConverter originResponseConverter;
  private final Map<UUID, Origin> responseCache;

  @Inject
  public OriginByIdResponseResolver(OriginResolver originResolver,
                                    OriginResponseConverter originResponseConverter,
                                    Map<UUID, Origin> responseCache) {
    this.originResolver = originResolver;
    this.originResponseConverter = originResponseConverter;
    this.responseCache = responseCache;
  }

  @Override
  public Origin apply(UUID id) {
    if (id == null) return null;
    return responseCache.computeIfAbsent(id, this::resolveUncached);
  }

  private Origin resolveUncached(UUID id) {
    return ObjectUtils.ifNotNull(originResolver.apply(id), originResponseConverter, Origin.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
