package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;

public class FactTypeConverterTest {

  private final Function<UUID, Namespace> namespaceConverter = id -> Namespace.builder().setId(id).setName("Namespace").build();
  private final Function<UUID, ObjectType> objectTypeConverter = id -> ObjectType.builder().setId(id).setName("ObjectType").build();
  private final Function<UUID, FactTypeEntity> factTypeEntityResolver = id -> new FactTypeEntity().setId(id);

  @Test
  public void testConvertFactTypeWithBothSourceAndDestination() {
    FactTypeEntity.FactObjectBindingDefinition binding1 = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(UUID.randomUUID())
            .setDestinationObjectTypeID(UUID.randomUUID());
    FactTypeEntity.FactObjectBindingDefinition binding2 = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(UUID.randomUUID())
            .setDestinationObjectTypeID(UUID.randomUUID())
            .setBidirectionalBinding(true);

    FactTypeEntity entity = createEntity()
            .addRelevantObjectBinding(binding1)
            .addRelevantObjectBinding(binding2);
    FactType model = createFactTypeConverter().apply(entity);
    assertModelCommon(entity, model);
    assertNotNull(model.getRelevantObjectBindings());
    assertRelevantObjectBindings(entity, model);
    assertNull(model.getRelevantFactBindings());
  }

  @Test
  public void testConvertFactTypeWithOnlySource() {
    FactTypeEntity entity = createEntity()
            .addRelevantObjectBinding(new FactTypeEntity.FactObjectBindingDefinition().setSourceObjectTypeID(UUID.randomUUID()));
    FactType model = createFactTypeConverter().apply(entity);
    assertNotNull(model.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertNull(model.getRelevantObjectBindings().get(0).getDestinationObjectType());
  }

  @Test
  public void testConvertFactTypeWithOnlyDestination() {
    FactTypeEntity entity = createEntity()
            .addRelevantObjectBinding(new FactTypeEntity.FactObjectBindingDefinition().setDestinationObjectTypeID(UUID.randomUUID()));
    FactType model = createFactTypeConverter().apply(entity);
    assertNull(model.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertNotNull(model.getRelevantObjectBindings().get(0).getDestinationObjectType());
  }

  @Test
  public void testConvertFactTypeWithFactBindings() {
    FactTypeEntity entity = createEntity()
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID()))
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID()));
    FactType model = createFactTypeConverter().apply(entity);
    assertModelCommon(entity, model);
    assertNull(model.getRelevantObjectBindings());
    assertNotNull(model.getRelevantFactBindings());
    assertRelevantFactBindings(entity, model);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createFactTypeConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutNamespaceConverterThrowsException() {
    FactTypeConverter.builder()
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeEntityResolver(factTypeEntityResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutObjectTypeConverterThrowsException() {
    FactTypeConverter.builder()
            .setNamespaceConverter(namespaceConverter)
            .setFactTypeEntityResolver(factTypeEntityResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutFactTypeEntityResolverThrowsException() {
    FactTypeConverter.builder()
            .setNamespaceConverter(namespaceConverter)
            .setObjectTypeConverter(objectTypeConverter)
            .build();
  }

  private FactTypeConverter createFactTypeConverter() {
    return FactTypeConverter.builder()
            .setNamespaceConverter(namespaceConverter)
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeEntityResolver(factTypeEntityResolver)
            .build();
  }

  private FactTypeEntity createEntity() {
    return new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName("FactType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter");
  }

  private void assertModelCommon(FactTypeEntity entity, FactType model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getName(), model.getName());
    assertEquals(entity.getValidator(), model.getValidator());
    assertEquals(entity.getValidatorParameter(), model.getValidatorParameter());

    assertNotNull(model.getNamespace());
    assertEquals(entity.getNamespaceID(), model.getNamespace().getId());
    assertEquals("Namespace", model.getNamespace().getName());
  }

  private void assertRelevantObjectBindings(FactTypeEntity entity, FactType model) {
    assertEquals(entity.getRelevantObjectBindings().size(), model.getRelevantObjectBindings().size());
    for (int i = 0; i < model.getRelevantObjectBindings().size(); i++) {
      FactType.FactObjectBindingDefinition modelBinding = model.getRelevantObjectBindings().get(i);
      FactTypeEntity.FactObjectBindingDefinition entityBinding = getBindingForSourceObjectTypeID(entity.getRelevantObjectBindings(), modelBinding.getSourceObjectType().getId());
      assertEquals(entityBinding.getSourceObjectTypeID(), modelBinding.getSourceObjectType().getId());
      assertEquals("ObjectType", modelBinding.getSourceObjectType().getName());
      assertEquals(entityBinding.getDestinationObjectTypeID(), modelBinding.getDestinationObjectType().getId());
      assertEquals("ObjectType", modelBinding.getDestinationObjectType().getName());
      assertEquals(entityBinding.isBidirectionalBinding(), modelBinding.isBidirectionalBinding());
    }
  }

  private void assertRelevantFactBindings(FactTypeEntity entity, FactType model) {
    Set<UUID> expected = SetUtils.set(entity.getRelevantFactBindings(), FactTypeEntity.MetaFactBindingDefinition::getFactTypeID);
    Set<UUID> actual = SetUtils.set(model.getRelevantFactBindings(), b -> b.getFactType().getId());
    assertEquals(expected, actual);
  }

  private FactTypeEntity.FactObjectBindingDefinition getBindingForSourceObjectTypeID(
          Set<FactTypeEntity.FactObjectBindingDefinition> bindings, UUID sourceObjectTypeID) {
    return bindings.stream()
            .filter(b -> Objects.equals(b.getSourceObjectTypeID(), sourceObjectTypeID))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
  }

}
