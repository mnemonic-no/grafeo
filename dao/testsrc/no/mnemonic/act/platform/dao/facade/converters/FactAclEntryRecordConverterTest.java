package no.mnemonic.act.platform.dao.facade.converters;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactAclEntryRecordConverterTest {

  private final FactAclEntryRecordConverter converter = new FactAclEntryRecordConverter();

  @Test
  public void testFromEntityWithNullEntity() {
    assertNull(converter.fromEntity(null));
  }

  @Test
  public void testFromEntityWithEmptyEntity() {
    assertNotNull(converter.fromEntity(new FactAclEntity()));
  }

  @Test
  public void testFromEntityWithFullEntity() {
    FactAclEntity entity = new FactAclEntity()
            .setId(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789L);

    FactAclEntryRecord record = converter.fromEntity(entity);
    assertNotNull(record);
    assertEquals(entity.getId(), record.getId());
    assertEquals(entity.getSubjectID(), record.getSubjectID());
    assertEquals(entity.getOriginID(), record.getOriginID());
    assertEquals(entity.getTimestamp(), record.getTimestamp());
  }

  @Test
  public void testToEntityWithNullRecord() {
    assertNull(converter.toEntity(null, UUID.randomUUID()));
  }

  @Test
  public void testToEntityWithEmptyRecord() {
    assertNotNull(converter.toEntity(new FactAclEntryRecord(), null));
  }

  @Test
  public void testToEntityWithFullRecord() {
    UUID factID = UUID.randomUUID();
    FactAclEntryRecord record = new FactAclEntryRecord()
            .setId(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789L);

    FactAclEntity entity = converter.toEntity(record, factID);
    assertNotNull(entity);
    assertEquals(factID, entity.getFactID());
    assertEquals(record.getId(), entity.getId());
    assertEquals(record.getSubjectID(), entity.getSubjectID());
    assertEquals(record.getOriginID(), entity.getOriginID());
    assertEquals(record.getTimestamp(), entity.getTimestamp());
  }
}
