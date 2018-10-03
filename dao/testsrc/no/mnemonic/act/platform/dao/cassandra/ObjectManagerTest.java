package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.dao.cassandra.entity.Direction;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

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

  @Test(expected = IllegalArgumentException.class)
  public void testSaveObjectTypeWithSameNameThrowsException() {
    getObjectManager().saveObjectType(createObjectType("objectType"));
    getObjectManager().saveObjectType(createObjectType("objectType"));
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

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectByTypeValueWithNonExistingObjectType() {
    ObjectEntity entity = createAndSaveObject(createAndSaveObjectType().getId());
    getObjectManager().getObject("nonExisting", entity.getValue());
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
  public void testFetchObjectsById() {
    ObjectEntity expected = createAndSaveObjects().get(0);
    List<ObjectEntity> actual = ListUtils.list(getObjectManager().getObjects(ListUtils.list(expected.getId())));
    assertEquals(1, actual.size());
    assertObject(expected, actual.get(0));
  }

  @Test
  public void testFetchObjectsByIdWithUnknownId() {
    assertEquals(0, ListUtils.list(getObjectManager().getObjects(null)).size());
    assertEquals(0, ListUtils.list(getObjectManager().getObjects(ListUtils.list())).size());
    assertEquals(0, ListUtils.list(getObjectManager().getObjects(ListUtils.list(UUID.randomUUID()))).size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveObjectWithNonExistingObjectType() {
    getObjectManager().saveObject(createObject());
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveSameObjectTwice() {
    ObjectTypeEntity type = createAndSaveObjectType();
    ObjectEntity object = createObject(type.getId());
    getObjectManager().saveObject(object);
    getObjectManager().saveObject(object);
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

    List<ObjectFactBindingEntity> actual = getObjectManager().fetchObjectFactBindings(object.getId());
    assertEquals(1, actual.size());
    assertObjectFactBinding(binding, actual.get(0));
  }

  @Test
  public void testFetchObjectFactBindingsWithNonExistingObject() {
    assertEquals(0, getObjectManager().fetchObjectFactBindings(null).size());
    assertEquals(0, getObjectManager().fetchObjectFactBindings(UUID.randomUUID()).size());
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

  @Test(expected = IllegalArgumentException.class)
  public void testSaveObjectFactBindingWithNonExistingObjectThrowsException() {
    getObjectManager().saveObjectFactBinding(createObjectFactBinding(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveObjectFactBindingTwiceThrowsException() {
    ObjectFactBindingEntity binding = createObjectFactBinding(createAndSaveObject().getId());
    getObjectManager().saveObjectFactBinding(binding);
    getObjectManager().saveObjectFactBinding(binding);
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
            .setValidatorParameter("validatorParameter");
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

  private List<ObjectEntity> createAndSaveObjects() {
    ObjectTypeEntity type = createAndSaveObjectType();
    List<ObjectEntity> entities = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      entities.add(getObjectManager().saveObject(new ObjectEntity()
              .setId(UUID.randomUUID())
              .setTypeID(type.getId())
              .setValue("object-" + i)
      ));
    }

    return entities;
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
