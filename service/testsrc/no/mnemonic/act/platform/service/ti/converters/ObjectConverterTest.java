package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsResult;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;

public class ObjectConverterTest {

  private final Function<UUID, ObjectType> objectTypeConverter = id -> ObjectType.builder().setId(id).build();
  private final Function<UUID, FactType> factTypeConverter = id -> FactType.builder().setId(id).build();
  private final Function<UUID, Collection<ObjectStatisticsResult.FactStatistic>> factStatisticsResolver = id -> null;

  @Test
  public void testConvertObject() {
    ObjectEntity entity = createEntity();
    assertModel(entity, createObjectConverter().apply(entity));
  }

  @Test
  public void testConvertObjectWithStatistics() {
    ObjectConverter converter = ObjectConverter.builder()
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeConverter(factTypeConverter)
            .setFactStatisticsResolver(id -> Collections.singleton(new ObjectStatisticsResult.FactStatistic(UUID.randomUUID(), 42, 123456789, 987654321)))
            .build();

    ObjectEntity entity = createEntity();
    Object model = converter.apply(entity);
    assertModel(entity, model);
    assertEquals(1, model.getStatistics().size());
    assertNotNull(model.getStatistics().get(0).getType());
    assertEquals(42, model.getStatistics().get(0).getCount());
    assertEquals(123456789, (long) model.getStatistics().get(0).getLastAddedTimestamp());
    assertEquals(987654321, (long) model.getStatistics().get(0).getLastSeenTimestamp());
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createObjectConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutObjectTypeConverterThrowsException() {
    ObjectConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setFactStatisticsResolver(factStatisticsResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutFactTypeConverterThrowsException() {
    ObjectConverter.builder()
            .setObjectTypeConverter(objectTypeConverter)
            .setFactStatisticsResolver(factStatisticsResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutFactStatisticsResolverThrowsException() {
    ObjectConverter.builder()
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeConverter(factTypeConverter)
            .build();
  }

  private ObjectConverter createObjectConverter() {
    return ObjectConverter.builder()
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeConverter(factTypeConverter)
            .setFactStatisticsResolver(factStatisticsResolver)
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
