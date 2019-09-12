package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.dao.cassandra.entity.FactCommentEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class FactCommentConverter implements Converter<FactCommentEntity, FactComment> {

  private final Function<UUID, Origin> originConverter;

  @Inject
  public FactCommentConverter(Function<UUID, Origin> originConverter) {
    this.originConverter = originConverter;
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
            .setOrigin(ObjectUtils.ifNotNull(originConverter.apply(entity.getOriginID()), Origin::toInfo))
            .setComment(entity.getComment())
            .setTimestamp(entity.getTimestamp())
            .build();
  }
}
