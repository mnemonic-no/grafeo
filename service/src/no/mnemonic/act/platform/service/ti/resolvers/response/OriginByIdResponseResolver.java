package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.service.ti.converters.response.OriginResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

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
