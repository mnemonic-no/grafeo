package no.mnemonic.services.grafeo.service.implementation.helpers;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.services.grafeo.api.request.v1.MetaFactBindingDefinition;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactTypeHelperTest {

  @Mock
  private FactManager factManager;
  @Mock
  private ObjectManager objectManager;
  @InjectMocks
  private FactTypeHelper helper;

  @Test
  public void testAssertFactTypeNotExistsSuccess() throws Exception {
    helper.assertFactTypeNotExists("type");
    verify(factManager).getFactType("type");
  }

  @Test
  public void testAssertFactTypeNotExistsFailure() {
    when(factManager.getFactType(anyString())).thenReturn(new FactTypeEntity());
    assertThrows(InvalidArgumentException.class, () -> helper.assertFactTypeNotExists("type"));
  }

  @Test
  public void testAssertObjectTypesToBindExistWithoutBindings() throws Exception {
    helper.assertObjectTypesToBindExist(null, "property");
    helper.assertObjectTypesToBindExist(ListUtils.list(), "property");
    verifyNoInteractions(objectManager);
  }

  @Test
  public void testAssertObjectTypesToBindSuccess() throws Exception {
    UUID sourceObjectType = UUID.randomUUID();
    UUID destinationObjectType = UUID.randomUUID();
    List<FactObjectBindingDefinition> definitions = ListUtils.list(new FactObjectBindingDefinition()
            .setSourceObjectType(sourceObjectType)
            .setDestinationObjectType(destinationObjectType)
    );

    when(objectManager.getObjectType(isA(UUID.class))).thenReturn(new ObjectTypeEntity());
    helper.assertObjectTypesToBindExist(definitions, "property");
    verify(objectManager).getObjectType(sourceObjectType);
    verify(objectManager).getObjectType(destinationObjectType);
  }

  @Test
  public void testAssertObjectTypesToBindExistWithoutSourceAndDestination() {
    List<FactObjectBindingDefinition> definitions = ListUtils.list(new FactObjectBindingDefinition());

    assertThrows(InvalidArgumentException.class, () -> helper.assertObjectTypesToBindExist(definitions, "property"));
    verifyNoInteractions(objectManager);
  }

  @Test
  public void testAssertObjectTypesToBindExistFailsOnSource() {
    UUID sourceObjectType = UUID.randomUUID();
    List<FactObjectBindingDefinition> definitions = ListUtils.list(new FactObjectBindingDefinition()
            .setSourceObjectType(sourceObjectType)
    );

    assertThrows(InvalidArgumentException.class, () -> helper.assertObjectTypesToBindExist(definitions, "property"));
    verify(objectManager).getObjectType(sourceObjectType);
  }

  @Test
  public void testAssertObjectTypesToBindExistFailsOnDestination() {
    UUID destinationObjectType = UUID.randomUUID();
    List<FactObjectBindingDefinition> definitions = ListUtils.list(new FactObjectBindingDefinition()
            .setDestinationObjectType(destinationObjectType)
    );

    assertThrows(InvalidArgumentException.class, () -> helper.assertObjectTypesToBindExist(definitions, "property"));
    verify(objectManager).getObjectType(destinationObjectType);
  }

  @Test
  public void testAssertFactTypesToBindExistWithoutBindings() throws Exception {
    helper.assertFactTypesToBindExist(null, "property");
    helper.assertFactTypesToBindExist(ListUtils.list(), "property");
    verifyNoInteractions(factManager);
  }

  @Test
  public void testAssertFactTypesToBindSuccess() throws Exception {
    UUID factType = UUID.randomUUID();
    List<MetaFactBindingDefinition> definitions = ListUtils.list(new MetaFactBindingDefinition()
            .setFactType(factType)
    );

    when(factManager.getFactType(isA(UUID.class))).thenReturn(new FactTypeEntity());
    helper.assertFactTypesToBindExist(definitions, "property");
    verify(factManager).getFactType(factType);
  }

  @Test
  public void testAssertFactTypesToBindExistFailure() {
    UUID factType = UUID.randomUUID();
    List<MetaFactBindingDefinition> definitions = ListUtils.list(new MetaFactBindingDefinition()
            .setFactType(factType)
    );

    assertThrows(InvalidArgumentException.class, () -> helper.assertFactTypesToBindExist(definitions, "property"));
    verify(factManager).getFactType(factType);
  }

  @Test
  public void testConvertFactObjectBindingDefinitionsWithoutBindings() {
    assertNull(helper.convertFactObjectBindingDefinitions(null));
    assertNull(helper.convertFactObjectBindingDefinitions(ListUtils.list()));
  }

  @Test
  public void testConvertFactObjectBindingDefinitionsWithBindings() {
    FactObjectBindingDefinition request1 = new FactObjectBindingDefinition()
            .setSourceObjectType(UUID.randomUUID())
            .setDestinationObjectType(UUID.randomUUID())
            .setBidirectionalBinding(false);
    FactObjectBindingDefinition request2 = new FactObjectBindingDefinition()
            .setSourceObjectType(UUID.randomUUID())
            .setDestinationObjectType(UUID.randomUUID())
            .setBidirectionalBinding(true);
    FactTypeEntity.FactObjectBindingDefinition entity1 = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(request1.getSourceObjectType())
            .setDestinationObjectTypeID(request1.getDestinationObjectType())
            .setBidirectionalBinding(request1.isBidirectionalBinding());
    FactTypeEntity.FactObjectBindingDefinition entity2 = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(request2.getSourceObjectType())
            .setDestinationObjectTypeID(request2.getDestinationObjectType())
            .setBidirectionalBinding(request2.isBidirectionalBinding());

    assertEquals(SetUtils.set(entity1, entity2), helper.convertFactObjectBindingDefinitions(ListUtils.list(request1, request2)));
  }

  @Test
  public void testConvertMetaFactBindingDefinitionsWithoutBindings() {
    assertNull(helper.convertMetaFactBindingDefinitions(null));
    assertNull(helper.convertMetaFactBindingDefinitions(ListUtils.list()));
  }

  @Test
  public void testConvertMetaFactBindingDefinitionsWithBindings() {
    MetaFactBindingDefinition request1 = new MetaFactBindingDefinition()
            .setFactType(UUID.randomUUID());
    MetaFactBindingDefinition request2 = new MetaFactBindingDefinition()
            .setFactType(UUID.randomUUID());
    FactTypeEntity.MetaFactBindingDefinition entity1 = new FactTypeEntity.MetaFactBindingDefinition()
            .setFactTypeID(request1.getFactType());
    FactTypeEntity.MetaFactBindingDefinition entity2 = new FactTypeEntity.MetaFactBindingDefinition()
            .setFactTypeID(request2.getFactType());

    assertEquals(SetUtils.set(entity1, entity2), helper.convertMetaFactBindingDefinitions(ListUtils.list(request1, request2)));
  }

}
