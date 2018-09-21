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
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class FactConverterTest {

  private final Function<UUID, FactType> factTypeConverter = id -> FactType.builder().setId(id).build();
  private final Function<UUID, Organization> organizationConverter = id -> Organization.builder().setId(id).build();
  private final Function<UUID, Source> sourceConverter = id -> Source.builder().setId(id).build();
  private final Function<UUID, Object> objectConverter = id -> Object.builder().setId(id).build();
  private final Function<UUID, FactEntity> factEntityResolver = id -> createEntity().setId(id);
  private final Predicate<FactEntity> accessChecker = fact -> true;

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createFactConverter().apply(null));
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityTwo() {
    FactEntity.FactObjectBinding source = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);
    FactEntity.FactObjectBinding destination = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactEntity entity = createEntity().setBindings(ListUtils.list(source, destination));

    Fact model = createFactConverter().apply(entity);

    assertModelCommon(entity, model);
    assertFalse(model.isBidirectionalBinding());
    assertNotNull(model.getSourceObject());
    assertEquals(source.getObjectID(), model.getSourceObject().getId());
    assertNotNull(model.getDestinationObject());
    assertEquals(destination.getObjectID(), model.getDestinationObject().getId());
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityTwoBidirectional() {
    FactEntity.FactObjectBinding source = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity.FactObjectBinding destination = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity entity = createEntity().setBindings(ListUtils.list(source, destination));

    Fact model = createFactConverter().apply(entity);

    assertModelCommon(entity, model);
    assertTrue(model.isBidirectionalBinding());
    assertNotNull(model.getSourceObject());
    assertEquals(source.getObjectID(), model.getSourceObject().getId());
    assertNotNull(model.getDestinationObject());
    assertEquals(destination.getObjectID(), model.getDestinationObject().getId());
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityOneFactIsSource() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactEntity entity = createEntity().setBindings(ListUtils.list(binding));

    Fact model = createFactConverter().apply(entity);

    assertModelCommon(entity, model);
    assertFalse(model.isBidirectionalBinding());
    assertNull(model.getSourceObject());
    assertNotNull(model.getDestinationObject());
    assertEquals(binding.getObjectID(), model.getDestinationObject().getId());
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityOneFactIsDestination() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);
    FactEntity entity = createEntity().setBindings(ListUtils.list(binding));

    Fact model = createFactConverter().apply(entity);

    assertModelCommon(entity, model);
    assertFalse(model.isBidirectionalBinding());
    assertNull(model.getDestinationObject());
    assertNotNull(model.getSourceObject());
    assertEquals(binding.getObjectID(), model.getSourceObject().getId());
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityOneBiDirectional() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity entity = createEntity().setBindings(ListUtils.list(binding));

    Fact model = createFactConverter().apply(entity);

    assertModelCommon(entity, model);
    assertTrue(model.isBidirectionalBinding());
    assertNotNull(model.getSourceObject());
    assertEquals(binding.getObjectID(), model.getSourceObject().getId());
    assertNotNull(model.getDestinationObject());
    assertEquals(binding.getObjectID(), model.getDestinationObject().getId());
  }

  @Test
  public void testConvertFactWithoutBinding() {
    FactEntity entity = createEntity();

    Fact model = createFactConverter().apply(entity);

    assertModelCommon(entity, model);
    assertNull(model.getSourceObject());
    assertNull(model.getDestinationObject());
  }

  @Test
  public void testConvertFactCannotResolveInReferenceToFact() {
    FactConverter converter = FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .setFactEntityResolver(id -> null)
            .setAccessChecker(accessChecker)
            .build();
    assertNull(converter.apply(createEntity()).getInReferenceTo());
  }

  @Test
  public void testConvertFactNoAccessToInReferenceToFact() {
    FactConverter converter = FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .setFactEntityResolver(factEntityResolver)
            .setAccessChecker(fact -> false)
            .build();
    assertNull(converter.apply(createEntity()).getInReferenceTo());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutFactTypeConverterThrowsException() {
    FactConverter.builder()
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .setFactEntityResolver(factEntityResolver)
            .setAccessChecker(accessChecker)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutOrganizationConverterThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .setFactEntityResolver(factEntityResolver)
            .setAccessChecker(accessChecker)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutSourceConverterThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setObjectConverter(objectConverter)
            .setFactEntityResolver(factEntityResolver)
            .setAccessChecker(accessChecker)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutObjectConverterThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setFactEntityResolver(factEntityResolver)
            .setAccessChecker(accessChecker)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutFactEntityResolverThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .setAccessChecker(accessChecker)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutAccessCheckerThrowsException() {
    FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .setFactEntityResolver(factEntityResolver)
            .build();
  }

  private FactConverter createFactConverter() {
    return FactConverter.builder()
            .setFactTypeConverter(factTypeConverter)
            .setOrganizationConverter(organizationConverter)
            .setSourceConverter(sourceConverter)
            .setObjectConverter(objectConverter)
            .setFactEntityResolver(factEntityResolver)
            .setAccessChecker(accessChecker)
            .build();
  }

  private FactEntity createEntity() {
    return new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321);
  }

  private void assertModelCommon(FactEntity entity, Fact model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getTypeID(), model.getType().getId());
    assertEquals(entity.getValue(), model.getValue());
    assertEquals(entity.getInReferenceToID(), model.getInReferenceTo().getId());
    assertEquals(entity.getOrganizationID(), model.getOrganization().getId());
    assertEquals(entity.getSourceID(), model.getSource().getId());
    assertEquals(entity.getAccessMode().name(), model.getAccessMode().name());
    assertEquals(entity.getTimestamp(), (long) model.getTimestamp());
    assertEquals(entity.getLastSeenTimestamp(), (long) model.getLastSeenTimestamp());
  }

}
