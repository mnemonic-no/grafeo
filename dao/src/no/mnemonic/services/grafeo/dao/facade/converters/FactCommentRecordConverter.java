package no.mnemonic.services.grafeo.dao.facade.converters;

import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactCommentEntity;

import java.util.UUID;

/**
 * Class for converting {@link FactCommentRecord}s.
 */
public class FactCommentRecordConverter {

  /**
   * Convert {@link FactCommentEntity} to {@link FactCommentRecord}.
   *
   * @param entity Comment to convert
   * @return Converted comment
   */
  public FactCommentRecord fromEntity(FactCommentEntity entity) {
    if (entity == null) return null;
    return new FactCommentRecord()
            .setId(entity.getId())
            .setReplyToID(entity.getReplyToID())
            .setOriginID(entity.getOriginID())
            .setComment(entity.getComment())
            .setTimestamp(entity.getTimestamp());
  }

  /**
   * Convert {@link FactCommentRecord} to {@link FactCommentEntity}.
   *
   * @param record Comment to convert
   * @param factID Fact the comment belongs to
   * @return Converted comment
   */
  public FactCommentEntity toEntity(FactCommentRecord record, UUID factID) {
    if (record == null) return null;
    return new FactCommentEntity()
            .setFactID(factID)
            .setId(record.getId())
            .setReplyToID(record.getReplyToID())
            .setOriginID(record.getOriginID())
            .setComment(record.getComment())
            .setTimestamp(record.getTimestamp());
  }
}
