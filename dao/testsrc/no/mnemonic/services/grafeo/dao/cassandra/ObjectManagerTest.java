package no.mnemonic.services.grafeo.dao.cassandra;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.grafeo.dao.cassandra.entity.Direction;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.exceptions.ImmutableViolationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectManagerTest extends AbstractManagerTest {

  @Test
  public void testSaveAndGetObjectTypeById() {
    ObjectTypeEntity entity = createAndSaveObjectType();
    assertObjectType(entity, getObjectManager().getObjectType(entity.getId()));
  }

  @Test
  public void testGetObjectTypeWithUnknownIdReturnsNull() {
    assertNull(getObjectManager().getObjectType((UUID) null));
    assertNull(getObjectManager().getObjectType(UUID.randomUUID()));
  }

  @Test
  public void testGetObjectTypeByIdTwiceReturnsSameInstance() {
    ObjectTypeEntity entity = createAndSaveObjectType();
    ObjectTypeEntity type1 = getObjectManager().getObjectType(entity.getId());
    ObjectTypeEntity type2 = getObjectManager().getObjectType(entity.getId());
    assertSame(type1, type2);
  }

  @Test
  public void testSaveObjectTypeTwiceInvalidatesIdCache() {
    ObjectTypeEntity entity = createObjectType();
    getObjectManager().saveObjectType(entity);
    ObjectTypeEntity type1 = getObjectManager().getObjectType(entity.getId());
    getObjectManager().saveObjectType(entity);
    ObjectTypeEntity type2 = getObjectManager().getObjectType(entity.getId());
    assertNotSame(type1, type2);
  }

  @Test
  public void testSaveAndGetObjectTypeByName() {
    ObjectTypeEntity entity = createAndSaveObjectType();
    assertObjectType(entity, getObjectManager().getObjectType(entity.getName()));
  }

  @Test
  public void testGetObjectTypeWithUnknownNameReturnsNull() {
    assertNull(getObjectManager().getObjectType((String) null));
    assertNull(getObjectManager().getObjectType(""));
    assertNull(getObjectManager().getObjectType("Unknown"));
  }

  @Test
  public void testGetObjectTypeByNameTwiceReturnsSameInstance() {
    ObjectTypeEntity entity = createAndSaveObjectType();
    ObjectTypeEntity type1 = getObjectManager().getObjectType(entity.getName());
    ObjectTypeEntity type2 = getObjectManager().getObjectType(entity.getName());
    assertSame(type1, type2);
  }

  @Test
  public void testSaveObjectTypeTwiceInvalidatesNameCache() {
    ObjectTypeEntity entity = createObjectType();
    getObjectManager().saveObjectType(entity);
    ObjectTypeEntity type1 = getObjectManager().getObjectType(entity.getName());
    getObjectManager().saveObjectType(entity);
    ObjectTypeEntity type2 = getObjectManager().getObjectType(entity.getName());
    assertNotSame(type1, type2);
  }

  @Test
  public void testSaveObjectTypeWithSameNameThrowsException() {
    assertDoesNotThrow(() -> getObjectManager().saveObjectType(createObjectType("objectType")));
    assertThrows(IllegalArgumentException.class, () -> getObjectManager().saveObjectType(createObjectType("objectType")));
  }

  @Test
  public void testSaveObjectTypeReturnsSameEntity() {
    ObjectTypeEntity entity = createObjectType();
    assertSame(entity, getObjectManager().saveObjectType(entity));
  }

  @Test
  public void testSaveObjectTypeReturnsNullOnNullInput() {
    assertNull(getObjectManager().saveObjectType(null));
  }

  @Test
  public void testFetchObjectTypes() {
    List<ObjectTypeEntity> expected = createAndSaveObjectTypes(3);
    List<ObjectTypeEntity> actual = getObjectManager().fetchObjectTypes();

    expected.sort(Comparator.comparing(ObjectTypeEntity::getId));
    actual.sort(Comparator.comparing(ObjectTypeEntity::getId));

    assertObjectTypes(expected, actual);
  }

  @Test
  public void testSaveAndGetObject() {
    ObjectEntity object = createAndSaveObject(createAndSaveObjectType().getId());
    assertObject(object, getObjectManager().getObject(object.getId()));
  }

  @Test
  public void testGetObjectWithNonExistingObject() {
    assertNull(getObjectManager().getObject(null));
    assertNull(getObjectManager().getObject(UUID.randomUUID()));
  }

  @Test
  public void testSaveAndGetObjectByTypeValue() {
    ObjectTypeEntity type = createAndSaveObjectType();
    ObjectEntity object = createAndSaveObject(type.getId());
    assertObject(object, getObjectManager().getObject(type.getName(), object.getValue()));
  }

  @Test
  public void testGetObjectByTypeValueWithNonExistingObjectType() {
    ObjectEntity entity = createAndSaveObject(createAndSaveObjectType().getId());
    assertThrows(IllegalArgumentException.class, () -> getObjectManager().getObject("nonExisting", entity.getValue()));
  }

  @Test
  public void testGetObjectByTypeValueWithNonExistingObjectValue() {
    ObjectTypeEntity entity = createAndSaveObjectType();
    assertNull(getObjectManager().getObject(entity.getName(), "nonExisting"));
  }

  @Test
  public void testGetObjectByTypeValueReturnsNullOnNullInput() {
    assertNull(getObjectManager().getObject(null, "ignored"));
    assertNull(getObjectManager().getObject("", "ignored"));
    assertNull(getObjectManager().getObject("ignored", null));
    assertNull(getObjectManager().getObject("ignored", ""));
  }

  @Test
  public void testSaveObjectWithNonExistingObjectType() {
    assertThrows(IllegalArgumentException.class, () -> getObjectManager().saveObject(createObject()));
  }

  @Test
  public void testSaveSameObjectTwice() {
    ObjectTypeEntity type = createAndSaveObjectType();
    ObjectEntity object = createObject(type.getId());
    assertDoesNotThrow(() -> getObjectManager().saveObject(object));
    assertThrows(ImmutableViolationException.class, () -> getObjectManager().saveObject(object));
  }

  @Test
  public void testSaveObjectReturnsSameEntity() {
    ObjectEntity entity = createObject(createAndSaveObjectType().getId());
    assertSame(entity, getObjectManager().saveObject(entity));
  }

  @Test
  public void testSaveObjectReturnsNullOnNullInput() {
    assertNull(getObjectManager().saveObject(null));
  }

  @Test
  public void testSaveAndFetchObjectFactBindings() {
    ObjectEntity object = createAndSaveObject(createAndSaveObjectType().getId());
    ObjectFactBindingEntity binding = createAndSaveObjectFactBinding(object.getId());

    List<ObjectFactBindingEntity> actual = ListUtils.list(getObjectManager().fetchObjectFactBindings(object.getId()));
    assertEquals(1, actual.size());
    assertObjectFactBinding(binding, actual.get(0));
  }

  @Test
  public void testFetchObjectFactBindingsWithNonExistingObject() {
    assertEquals(0, ListUtils.list(getObjectManager().fetchObjectFactBindings(null)).size());
    assertEquals(0, ListUtils.list(getObjectManager().fetchObjectFactBindings(UUID.randomUUID())).size());
  }

  @Test
  public void testSaveObjectFactBindingReturnsSameEntity() {
    ObjectFactBindingEntity binding = createObjectFactBinding(createAndSaveObject().getId());
    assertSame(binding, getObjectManager().saveObjectFactBinding(binding));
  }

  @Test
  public void testSaveObjectFactBindingReturnsNullOnNullInput() {
    assertNull(getObjectManager().saveObjectFactBinding(null));
  }

  @Test
  public void testSaveObjectFactBindingWithNonExistingObjectThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> getObjectManager().saveObjectFactBinding(createObjectFactBinding(UUID.randomUUID())));
  }

  @Test
  public void testSaveObjectFactBindingTwiceThrowsException() {
    ObjectFactBindingEntity binding = createObjectFactBinding(createAndSaveObject().getId());
    assertDoesNotThrow(() -> getObjectManager().saveObjectFactBinding(binding));
    assertThrows(ImmutableViolationException.class, () -> getObjectManager().saveObjectFactBinding(binding));
  }

  private ObjectTypeEntity createObjectType() {
    return createObjectType("objectType");
  }

  private ObjectTypeEntity createObjectType(String name) {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName(name)
            .setValidator("validator")
            .setValidatorParameter("validatorParameter")
            .addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
  }

  private ObjectEntity createObject() {
    return createObject(UUID.randomUUID());
  }

  private ObjectEntity createObject(UUID typeID) {
    return new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(typeID)
            .setValue("test");
  }

  private ObjectFactBindingEntity createObjectFactBinding(UUID objectID) {
    return new ObjectFactBindingEntity()
            .setObjectID(objectID)
            .setFactID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
  }

  private ObjectTypeEntity createAndSaveObjectType() {
    return createAndSaveObjectTypes(1).get(0);
  }

  private List<ObjectTypeEntity> createAndSaveObjectTypes(int numberOfEntities) {
    List<ObjectTypeEntity> entities = new ArrayList<>();

    for (int i = 0; i < numberOfEntities; i++) {
      entities.add(getObjectManager().saveObjectType(createObjectType("objectType" + i)));
    }

    return entities;
  }

  private ObjectEntity createAndSaveObject() {
    return createAndSaveObject(createAndSaveObjectType().getId());
  }

  private ObjectEntity createAndSaveObject(UUID typeID) {
    return getObjectManager().saveObject(createObject(typeID));
  }

  private ObjectFactBindingEntity createAndSaveObjectFactBinding(UUID objectID) {
    return getObjectManager().saveObjectFactBinding(createObjectFactBinding(objectID));
  }

  private void assertObjectType(ObjectTypeEntity expected, ObjectTypeEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getNamespaceID(), actual.getNamespaceID());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getValidator(), actual.getValidator());
    assertEquals(expected.getValidatorParameter(), actual.getValidatorParameter());
    assertEquals(expected.getFlags(), actual.getFlags());
  }

  private void assertObjectTypes(List<ObjectTypeEntity> expected, List<ObjectTypeEntity> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      assertObjectType(expected.get(i), actual.get(i));
    }
  }

  private void assertObject(ObjectEntity expected, ObjectEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
  }

  private void assertObjectFactBinding(ObjectFactBindingEntity expected, ObjectFactBindingEntity actual) {
    assertEquals(expected.getObjectID(), actual.getObjectID());
    assertEquals(expected.getFactID(), actual.getFactID());
    assertEquals(expected.getDirection(), actual.getDirection());
  }

}
