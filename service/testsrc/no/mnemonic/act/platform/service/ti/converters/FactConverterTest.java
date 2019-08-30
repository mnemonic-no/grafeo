package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.Direction;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;

public class FactConverterTest {

  private final Function<UUID, FactType> factTypeConverter = id -> FactType.builder().setId(id).build();
  private final Function<UUID, Organization> organizationConverter = id -> Organization.builder().setId(id).build();
  private final Function<UUID, Subject> subjectConverter = id -> Subject.builder().setId(id).build();
  private final Function<UUID, Origin> originConverter = id -> Origin.builder().setId(id).build();
  private final Function<UUID, Object> objectConverter = id -> Object.builder().setId(id).build();
  private final Function<UUID, FactEntity> factEntityResolver = id -> createEntity().setId(id);
  private final Predicate<FactEntity> accessChecker = fact -> true;
  private final Predicate<FactEntity> retractionChecker = fact -> false;
  private final FactConverter converter = new FactConverter(factTypeConverter, organizationConverter, subjectConverter,
          originConverter, objectConverter, factEntityResolver, accessChecker, retractionChecker);

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
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

    Fact model = converter.apply(entity);

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

    Fact model = converter.apply(entity);

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

    Fact model = converter.apply(entity);

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

    Fact model = converter.apply(entity);

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

    Fact model = converter.apply(entity);

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

    Fact model = converter.apply(entity);

    assertModelCommon(entity, model);
    assertNull(model.getSourceObject());
    assertNull(model.getDestinationObject());
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityTwoAndSameDirectionFactIsDestination() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);
    FactEntity entity = createEntity().setBindings(ListUtils.list(binding, binding));

    Fact model = converter.apply(entity);

    assertModelCommon(entity, model);
    assertNull(model.getSourceObject());
    assertNull(model.getDestinationObject());
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityTwoAndSameDirectionFactIsSource() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactEntity entity = createEntity().setBindings(ListUtils.list(binding, binding));

    Fact model = converter.apply(entity);

    assertModelCommon(entity, model);
    assertNull(model.getSourceObject());
    assertNull(model.getDestinationObject());
  }

  @Test
  public void testConvertFactWithBindingOfCardinalityTree() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity entity = createEntity().setBindings(ListUtils.list(binding, binding, binding));

    Fact model = converter.apply(entity);

    assertModelCommon(entity, model);
    assertNull(model.getSourceObject());
    assertNull(model.getDestinationObject());
  }

  @Test
  public void testConvertFactCannotResolveInReferenceToFact() {
    FactConverter converter = new FactConverter(factTypeConverter, organizationConverter, subjectConverter, originConverter,
            objectConverter, id -> null, accessChecker, retractionChecker);
    assertNull(converter.apply(createEntity()).getInReferenceTo());
  }

  @Test
  public void testConvertFactNoAccessToInReferenceToFact() {
    FactConverter converter = new FactConverter(factTypeConverter, organizationConverter, subjectConverter, originConverter,
            objectConverter, factEntityResolver, fact -> false, retractionChecker);
    assertNull(converter.apply(createEntity()).getInReferenceTo());
  }

  @Test
  public void testConvertRetractedFact() {
    FactConverter converter = new FactConverter(factTypeConverter, organizationConverter, subjectConverter, originConverter,
            objectConverter, factEntityResolver, accessChecker, fact -> true);
    assertEquals(set(Fact.Flag.Retracted), converter.apply(createEntity()).getFlags());
  }

  private FactEntity createEntity() {
    return new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setTrust(0.1f)
            .setConfidence(0.2f)
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
    assertEquals(entity.getAddedByID(), model.getAddedBy().getId());
    assertEquals(entity.getSourceID(), model.getOrigin().getId());
    assertEquals(entity.getTrust(), model.getTrust(), 0.0);
    assertEquals(entity.getConfidence(), model.getConfidence(), 0.0);
    assertEquals(entity.getAccessMode().name(), model.getAccessMode().name());
    assertEquals(entity.getTimestamp(), (long) model.getTimestamp());
    assertEquals(entity.getLastSeenTimestamp(), (long) model.getLastSeenTimestamp());
    assertEquals(set(), model.getFlags());
  }
}
