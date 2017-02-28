package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.model.v1.Source;
import no.mnemonic.act.platform.entity.cassandra.FactCommentEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.UUID;
import java.util.function.Function;

public class FactCommentConverter implements Converter<FactCommentEntity, FactComment> {

  private final Function<UUID, Source> sourceConverter;

  private FactCommentConverter(Function<UUID, Source> sourceConverter) {
    this.sourceConverter = sourceConverter;
  }

  @Override
  public Class<FactCommentEntity> getSourceType() {
    return FactCommentEntity.class;
  }

  @Override
  public Class<FactComment> getTargetType() {
    return FactComment.class;
  }

  @Override
  public FactComment apply(FactCommentEntity entity) {
    if (entity == null) return null;
    return FactComment.builder()
            .setId(entity.getId())
            .setReplyTo(entity.getReplyToID())
            .setSource(ObjectUtils.ifNotNull(sourceConverter.apply(entity.getSourceID()), Source::toInfo))
            .setComment(entity.getComment())
            .setTimestamp(entity.getTimestamp())
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Function<UUID, Source> sourceConverter;

    private Builder() {
    }

    public FactCommentConverter build() {
      ObjectUtils.notNull(sourceConverter, "Cannot instantiate FactCommentConverter without 'sourceConverter'.");
      return new FactCommentConverter(sourceConverter);
    }

    public Builder setSourceConverter(Function<UUID, Source> sourceConverter) {
      this.sourceConverter = sourceConverter;
      return this;
    }
  }

}
