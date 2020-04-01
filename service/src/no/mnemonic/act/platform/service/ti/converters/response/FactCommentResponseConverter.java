package no.mnemonic.act.platform.service.ti.converters.response;

import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.service.ti.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.function.Function;

public class FactCommentResponseConverter implements Function<FactCommentRecord, FactComment> {

  private final OriginByIdResponseResolver originConverter;

  @Inject
  public FactCommentResponseConverter(OriginByIdResponseResolver originConverter) {
    this.originConverter = originConverter;
  }

  @Override
  public FactComment apply(FactCommentRecord record) {
    if (record == null) return null;
    return FactComment.builder()
            .setId(record.getId())
            .setReplyTo(record.getReplyToID())
            .setOrigin(ObjectUtils.ifNotNull(originConverter.apply(record.getOriginID()), Origin::toInfo))
            .setComment(record.getComment())
            .setTimestamp(record.getTimestamp())
            .build();
  }
}
