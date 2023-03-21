package no.mnemonic.services.grafeo.service.ti.converters.response;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.model.v1.Namespace;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.NamespaceByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.ObjectTypeByIdResponseResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FactTypeResponseConverterTest {

  private FactTypeResponseConverter converter;

  @Before
  public void setUp() {
    NamespaceByIdResponseResolver namespaceConverter = mock(NamespaceByIdResponseResolver.class);
    when(namespaceConverter.apply(notNull())).thenAnswer(i -> Namespace.builder().setId(i.getArgument(0)).setName("Namespace").build());

    ObjectTypeByIdResponseResolver objectTypeConverter = mock(ObjectTypeByIdResponseResolver.class);
    when(objectTypeConverter.apply(notNull())).thenAnswer(i -> ObjectType.builder().setId(i.getArgument(0)).setName("ObjectType").build());

    FactManager factManager = mock(FactManager.class);
    when(factManager.getFactType(isA(UUID.class))).thenAnswer(i -> new FactTypeEntity().setId(i.getArgument(0)));

    converter = new FactTypeResponseConverter(namespaceConverter, objectTypeConverter, factManager);
  }

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
    FactType model = converter.apply(entity);
    assertModelCommon(entity, model);
    assertNotNull(model.getRelevantObjectBindings());
    assertRelevantObjectBindings(entity, model);
    assertNull(model.getRelevantFactBindings());
  }

  @Test
  public void testConvertFactTypeWithOnlySource() {
    FactTypeEntity entity = createEntity()
      .addRelevantObjectBinding(new FactTypeEntity.FactObjectBindingDefinition().setSourceObjectTypeID(UUID.randomUUID()));
    FactType model = converter.apply(entity);
    assertNotNull(model.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertNull(model.getRelevantObjectBindings().get(0).getDestinationObjectType());
  }

  @Test
  public void testConvertFactTypeWithOnlyDestination() {
    FactTypeEntity entity = createEntity()
      .addRelevantObjectBinding(new FactTypeEntity.FactObjectBindingDefinition().setDestinationObjectTypeID(UUID.randomUUID()));
    FactType model = converter.apply(entity);
    assertNull(model.getRelevantObjectBindings().get(0).getSourceObjectType());
    assertNotNull(model.getRelevantObjectBindings().get(0).getDestinationObjectType());
  }

  @Test
  public void testConvertFactTypeWithFactBindings() {
    FactTypeEntity entity = createEntity()
      .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID()))
      .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(UUID.randomUUID()));
    FactType model = converter.apply(entity);
    assertModelCommon(entity, model);
    assertNull(model.getRelevantObjectBindings());
    assertNotNull(model.getRelevantFactBindings());
    assertRelevantFactBindings(entity, model);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  private FactTypeEntity createEntity() {
    return new FactTypeEntity()
      .setId(UUID.randomUUID())
      .setNamespaceID(UUID.randomUUID())
      .setName("FactType")
      .setDefaultConfidence(0.1f)
      .setValidator("Validator")
      .setValidatorParameter("ValidatorParameter");
  }

  private void assertModelCommon(FactTypeEntity entity, FactType model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getName(), model.getName());
    assertEquals(entity.getDefaultConfidence(), model.getDefaultConfidence(), 0.0);
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
