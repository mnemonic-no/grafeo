package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OriginByIdConverter implements Converter<UUID, Origin> {

  private final Function<UUID, OriginEntity> originResolver;
  private final Function<OriginEntity, Origin> originConverter;

  @Inject
  public OriginByIdConverter(Function<UUID, OriginEntity> originResolver, Function<OriginEntity, Origin> originConverter) {
    this.originResolver = originResolver;
    this.originConverter = originConverter;
  }

  @Override
  public Class<UUID> getSourceType() {
    return UUID.class;
  }

  @Override
  public Class<Origin> getTargetType() {
    return Origin.class;
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
