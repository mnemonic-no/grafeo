package no.mnemonic.services.grafeo.dao.facade.converters;

import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactAclEntity;

import java.util.UUID;

/**
 * Class for converting {@link FactAclEntryRecord}s.
 */
public class FactAclEntryRecordConverter {

  /**
   * Convert {@link FactAclEntity} to {@link FactAclEntryRecord}.
   *
   * @param entity ACL entry to convert
   * @return Converted ACL entry
   */
  public FactAclEntryRecord fromEntity(FactAclEntity entity) {
    if (entity == null) return null;
    return new FactAclEntryRecord()
            .setId(entity.getId())
            .setSubjectID(entity.getSubjectID())
            .setOriginID(entity.getOriginID())
            .setTimestamp(entity.getTimestamp());
  }

  /**
   * Convert {@link FactAclEntryRecord} to {@link FactAclEntity}.
   *
   * @param record ACL entry to convert
   * @param factID Fact the ACL entry belongs to
   * @return Converted ACL entry
   */
  public FactAclEntity toEntity(FactAclEntryRecord record, UUID factID) {
    if (record == null) return null;
    return new FactAclEntity()
            .setFactID(factID)
            .setId(record.getId())
            .setSubjectID(record.getSubjectID())
            .setOriginID(record.getOriginID())
            .setTimestamp(record.getTimestamp());
  }
}
