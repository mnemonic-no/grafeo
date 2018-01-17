package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.Direction;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FactConverterTest {

  private final Function<UUID, FactType> factTypeConverter = id -> FactType.builder().setId(id).build();
  private final Function<UUID, Fact> inReferenceToConverter = id -> Fact.builder().setId(id).build();
  private final Function<UUID, Organization> organizationConverter = id -> Organization.builder().setId(id).build();
  private final Function<UUID, Source> sourceConverter = id -> Source.builder().setId(id).build();
  private final Function<UUID, Object> objectConverter = id -> Object.builder().setId(id).build();

  @Test
  public void testConvertFact() {
    FactEntity entity = createEntity();
    assertModel(entity, createFactConverter().apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createFactConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutFactTypeConverterThrowsException() {
    FactConverter.builder()
            .setInReferenceToConverter(inReferenceToConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutInReferenceToConverterThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutOrganizationConverterThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setInReferenceToConverter(inReferenceToConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutSourceConverterThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setInReferenceToConverter(inReferenceToConverter)
            .setOrganizationConverter(organizationConverter)
            .setObjectConverter(objectConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutObjectConverterThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setInReferenceToConverter(inReferenceToConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .build();
  }

  private FactConverter createFactConverter() {
    return FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setInReferenceToConverter(inReferenceToConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .build();
  }

  private FactEntity createEntity() {
    FactEntity.FactObjectBinding binding1 = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactEntity.FactObjectBinding binding2 = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);

    return new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setBindings(ListUtils.list(binding1, binding2));
  }

  private void assertModel(FactEntity entity, Fact model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getTypeID(), model.getType().getId());
    assertEquals(entity.getValue(), model.getValue());
    assertEquals(entity.getInReferenceToID(), model.getInReferenceTo().getId());
    assertEquals(entity.getOrganizationID(), model.getOrganization().getId());
    assertEquals(entity.getSourceID(), model.getSource().getId());
    assertEquals(entity.getAccessMode().name(), model.getAccessMode().name());
    assertEquals(entity.getTimestamp(), (long) model.getTimestamp());
    assertEquals(entity.getLastSeenTimestamp(), (long) model.getLastSeenTimestamp());

    assertEquals(entity.getBindings().size(), model.getObjects().size());
    for (int i = 0; i < entity.getBindings().size(); i++) {
      FactEntity.FactObjectBinding entityBinding = entity.getBindings().get(i);
      Fact.FactObjectBinding modelBinding = model.getObjects().get(i);
      assertEquals(entityBinding.getObjectID(), modelBinding.getObject().getId());
      assertEquals(entityBinding.getDirection().name(), modelBinding.getDirection().name());
    }
  }

}
