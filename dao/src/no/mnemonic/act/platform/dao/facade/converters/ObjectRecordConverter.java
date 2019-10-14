package no.mnemonic.act.platform.dao.facade.converters;

import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;

/**
 * Class for converting {@link ObjectRecord}s.
 */
public class ObjectRecordConverter {

  /**
   * Convert {@link ObjectEntity} to {@link ObjectRecord}.
   *
   * @param entity Object to convert
   * @return Converted Object
   */
  public ObjectRecord fromEntity(ObjectEntity entity) {
    if (entity == null) return null;
    return new ObjectRecord()
            .setId(entity.getId())
            .setTypeID(entity.getTypeID())
            .setValue(entity.getValue());
  }

  /**
   * Convert {@link ObjectRecord} to {@link ObjectEntity}.
   *
   * @param record Object to convert
   * @return Converted Object
   */
  public ObjectEntity toEntity(ObjectRecord record) {
    if (record == null) return null;
    return new ObjectEntity()
            .setId(record.getId())
            .setTypeID(record.getTypeID())
            .setValue(record.getValue());
  }
}
