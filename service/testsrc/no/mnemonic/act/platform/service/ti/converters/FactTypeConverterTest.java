package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;

public class FactTypeConverterTest {

  private final Function<UUID, Namespace> namespaceConverter = id -> Namespace.builder().setId(id).setName("Namespace").build();
  private final Function<UUID, ObjectType> objectTypeConverter = id -> ObjectType.builder().setId(id).setName("ObjectType").build();

  @Test
  public void testConvertFactTypeWithBothSourceAndDestination() {
    FactTypeEntity entity = createEntity();
    assertModel(entity, createFactTypeConverter().apply(entity));
  }

  @Test
  public void testConvertFactTypeWithOnlySource() {
    FactTypeEntity entity = createEntity()
            .setRelevantObjectBindings(ListUtils.list(new FactTypeEntity.FactObjectBindingDefinition()
                    .setSourceObjectTypeID(UUID.randomUUID())));
    FactType model = createFactTypeConverter().apply(entity);
    assertNotNull(model.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertNull(model.getRelevantObjectBindings().get(0).getDestinationObjectType());
  }

  @Test
  public void testConvertFactTypeWithOnlyDestination() {
    FactTypeEntity entity = createEntity()
            .setRelevantObjectBindings(ListUtils.list(new FactTypeEntity.FactObjectBindingDefinition()
                    .setDestinationObjectTypeID(UUID.randomUUID())));
    FactType model = createFactTypeConverter().apply(entity);
    assertNull(model.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertNotNull(model.getRelevantObjectBindings().get(0).getDestinationObjectType());
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createFactTypeConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutNamespaceConverterThrowsException() {
    FactTypeConverter.builder().setObjectTypeConverter(objectTypeConverter).build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutObjectTypeConverterThrowsException() {
    FactTypeConverter.builder().setNamespaceConverter(namespaceConverter).build();
  }

  private FactTypeConverter createFactTypeConverter() {
    return FactTypeConverter.builder()
            .setNamespaceConverter(namespaceConverter)
            .setObjectTypeConverter(objectTypeConverter)
            .build();
  }

  private FactTypeEntity createEntity() {
    FactTypeEntity.FactObjectBindingDefinition binding1 = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(UUID.randomUUID())
            .setDestinationObjectTypeID(UUID.randomUUID());
    FactTypeEntity.FactObjectBindingDefinition binding2 = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(UUID.randomUUID())
            .setDestinationObjectTypeID(UUID.randomUUID())
            .setBidirectionalBinding(true);

    return new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName("FactType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .setEntityHandler("EntityHandler")
            .setEntityHandlerParameter("EntityHandlerParameter")
            .setRelevantObjectBindings(ListUtils.list(binding1, binding2));
  }

  private void assertModel(FactTypeEntity entity, FactType model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getName(), model.getName());
    assertEquals(entity.getValidator(), model.getValidator());
    assertEquals(entity.getValidatorParameter(), model.getValidatorParameter());
    assertEquals(entity.getEntityHandler(), model.getEntityHandler());
    assertEquals(entity.getEntityHandlerParameter(), model.getEntityHandlerParameter());

    assertNotNull(model.getNamespace());
    assertEquals(entity.getNamespaceID(), model.getNamespace().getId());
    assertEquals("Namespace", model.getNamespace().getName());

    assertNotNull(model.getRelevantObjectBindings());
    assertEquals(entity.getRelevantObjectBindings().size(), model.getRelevantObjectBindings().size());
    for (int i = 0; i < entity.getRelevantObjectBindings().size(); i++) {
      FactTypeEntity.FactObjectBindingDefinition entityBinding = entity.getRelevantObjectBindings().get(i);
      FactType.FactObjectBindingDefinition modelBinding = model.getRelevantObjectBindings().get(i);
      assertEquals(entityBinding.getSourceObjectTypeID(), modelBinding.getSourceObjectType().getId());
      assertEquals("ObjectType", modelBinding.getSourceObjectType().getName());
      assertEquals(entityBinding.getDestinationObjectTypeID(), modelBinding.getDestinationObjectType().getId());
      assertEquals("ObjectType", modelBinding.getDestinationObjectType().getName());
      assertEquals(entityBinding.isBidirectionalBinding(), modelBinding.isBidirectionalBinding());
    }
  }

}
