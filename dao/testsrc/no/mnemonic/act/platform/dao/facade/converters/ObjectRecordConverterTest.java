package no.mnemonic.act.platform.dao.facade.converters;

import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class ObjectRecordConverterTest {

  private final ObjectRecordConverter converter = new ObjectRecordConverter();

  @Test
  public void testFromEntityWithNullObject() {
    assertNull(converter.fromEntity(null));
  }

  @Test
  public void testFromEntityWithEmptyObject() {
    assertNotNull(converter.fromEntity(new ObjectEntity()));
  }

  @Test
  public void testFromEntityWithFullObject() {
    ObjectEntity entity = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");

    ObjectRecord record = converter.fromEntity(entity);
    assertNotNull(record);
    assertEquals(entity.getId(), record.getId());
    assertEquals(entity.getTypeID(), record.getTypeID());
    assertEquals(entity.getValue(), record.getValue());
  }

  @Test
  public void testToEntityWithNullObject() {
    assertNull(converter.toEntity(null));
  }

  @Test
  public void testToEntityWithEmptyObject() {
    assertNotNull(converter.toEntity(new ObjectRecord()));
  }

  @Test
  public void testToEntityWithFullObject() {
    ObjectRecord record = new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");

    ObjectEntity entity = converter.toEntity(record);
    assertNotNull(record);
    assertEquals(record.getId(), entity.getId());
    assertEquals(record.getTypeID(), entity.getTypeID());
    assertEquals(record.getValue(), entity.getValue());
  }
}
