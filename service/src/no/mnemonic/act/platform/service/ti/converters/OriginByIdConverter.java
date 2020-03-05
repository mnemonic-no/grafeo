package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OriginByIdConverter implements Function<UUID, Origin> {

  private final OriginResolver originResolver;
  private final OriginConverter originConverter;

  @Inject
  public OriginByIdConverter(OriginResolver originResolver, OriginConverter originConverter) {
    this.originResolver = originResolver;
    this.originConverter = originConverter;
  }

  @Override
  public Origin apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNotNull(originResolver.apply(id), originConverter, Origin.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
