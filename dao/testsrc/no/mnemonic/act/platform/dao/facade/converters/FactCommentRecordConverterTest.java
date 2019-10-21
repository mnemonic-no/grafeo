package no.mnemonic.act.platform.dao.facade.converters;

import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactCommentEntity;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class FactCommentRecordConverterTest {

  private final FactCommentRecordConverter converter = new FactCommentRecordConverter();

  @Test
  public void testFromEntityWithNullEntity() {
    assertNull(converter.fromEntity(null));
  }

  @Test
  public void testFromEntityWithEmptyEntity() {
    assertNotNull(converter.fromEntity(new FactCommentEntity()));
  }

  @Test
  public void testFromEntityWithFullEntity() {
    FactCommentEntity entity = new FactCommentEntity()
            .setId(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setComment("Hello World!")
            .setTimestamp(123456789L);

    FactCommentRecord record = converter.fromEntity(entity);
    assertNotNull(record);
    assertEquals(entity.getId(), record.getId());
    assertEquals(entity.getReplyToID(), record.getReplyToID());
    assertEquals(entity.getOriginID(), record.getOriginID());
    assertEquals(entity.getComment(), record.getComment());
    assertEquals(entity.getTimestamp(), record.getTimestamp());
  }

  @Test
  public void testToEntityWithNullRecord() {
    assertNull(converter.toEntity(null, UUID.randomUUID()));
  }

  @Test
  public void testToEntityWithEmptyRecord() {
    assertNotNull(converter.toEntity(new FactCommentRecord(), null));
  }

  @Test
  public void testToEntityWithFullRecord() {
    UUID factID = UUID.randomUUID();
    FactCommentRecord record = new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setComment("Hello World!")
            .setTimestamp(123456789L);

    FactCommentEntity entity = converter.toEntity(record, factID);
    assertNotNull(entity);
    assertEquals(factID, entity.getFactID());
    assertEquals(record.getId(), entity.getId());
    assertEquals(record.getReplyToID(), entity.getReplyToID());
    assertEquals(record.getOriginID(), entity.getOriginID());
    assertEquals(record.getComment(), entity.getComment());
    assertEquals(record.getTimestamp(), entity.getTimestamp());
  }
}
