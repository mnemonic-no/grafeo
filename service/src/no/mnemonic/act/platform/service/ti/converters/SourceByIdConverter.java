package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Source;

import java.util.UUID;

public class SourceByIdConverter implements Converter<UUID, Source> {

  @Override
  public Class<UUID> getSourceType() {
    return UUID.class;
  }

  @Override
  public Class<Source> getTargetType() {
    return Source.class;
  }

  @Override
  public Source apply(UUID id) {
    if (id == null) return null;
    // For now just return a static Source.
    return Source.builder()
            .setId(id)
            .setName("Not implemented yet!")
            .build();
  }
}
