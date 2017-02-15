package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ObjectConverterTest {

  private final Function<UUID, ObjectType> objectTypeConverter = id -> ObjectType.builder().setId(id).build();

  @Test
  public void testConvertObject() {
    ObjectEntity entity = createEntity();
    assertModel(entity, createObjectConverter().apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createObjectConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutObjectTypeConverterThrowsException() {
    ObjectConverter.builder().build();
  }

  private ObjectConverter createObjectConverter() {
    return ObjectConverter.builder()
            .setObjectTypeConverter(objectTypeConverter)
            .build();
  }

  private ObjectEntity createEntity() {
    return new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");
  }

  private void assertModel(ObjectEntity entity, Object model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getTypeID(), model.getType().getId());
    assertEquals(entity.getValue(), model.getValue());
  }

}
