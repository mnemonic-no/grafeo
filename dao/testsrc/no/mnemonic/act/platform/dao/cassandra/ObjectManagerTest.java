package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.entity.cassandra.Direction;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ObjectManagerTest extends AbstractManagerTest {

  @Test
  public void testSaveAndGetObjectType() {
    ObjectTypeEntity entity = createObjectType();

    getObjectManager().saveObjectType(entity);
    assertObjectType(entity, getObjectManager().getObjectType(entity.getId()));
  }

  @Test
  public void testFetchObjectTypes() {
    List<ObjectTypeEntity> expected = createAndSaveObjectTypes(3);
    List<ObjectTypeEntity> actual = getObjectManager().fetchObjectTypes();

    Comparator<ObjectTypeEntity> comparator = (e1, e2) -> e1.getId().compareTo(e2.getId());
    expected.sort(comparator);
    actual.sort(comparator);

    assertObjectTypes(expected, actual);
  }

  @Test
  public void testSaveAndGetObject() throws ImmutableViolationException {
    ObjectEntity entity = createObject();

    getObjectManager().saveObject(entity);
    assertObject(entity, getObjectManager().getObject(entity.getId()));
  }

  @Test
  public void testSaveAndGetObjectByTypeValue() throws ImmutableViolationException {
    ObjectTypeEntity type = createObjectType();
    ObjectEntity object = createObject(type.getId());

    getObjectManager().saveObjectType(type);
    getObjectManager().saveObject(object);
    assertObject(object, getObjectManager().getObject(type.getName(), object.getValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectByTypeValueWithNonExistingObjectType() throws ImmutableViolationException {
    ObjectEntity entity = createObject();

    getObjectManager().saveObject(entity);
    getObjectManager().getObject("nonExisting", entity.getValue());
  }

  @Test
  public void testGetObjectByTypeValueWithNonExistingObjectValue() throws ImmutableViolationException {
    ObjectTypeEntity type = createObjectType();
    ObjectEntity object = createObject(type.getId());

    getObjectManager().saveObjectType(type);
    getObjectManager().saveObject(object);
    assertNull(getObjectManager().getObject(type.getName(), "nonExisting"));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveSameObjectTwice() throws ImmutableViolationException {
    ObjectEntity entity = createObject();
    getObjectManager().saveObject(entity);
    getObjectManager().saveObject(entity);
  }

  @Test
  public void testSaveAndFetchObjectFactBindings() throws ImmutableViolationException {
    ObjectEntity object = createObject();
    ObjectFactBindingEntity binding = createObjectFactBinding(object.getId());

    getObjectManager().saveObject(object);
    getObjectManager().saveObjectFactBinding(binding);

    List<ObjectFactBindingEntity> actual = getObjectManager().fetchObjectFactBindings(object.getId());
    assertEquals(1, actual.size());
    assertObjectFactBinding(binding, actual.get(0));
  }

  @Test
  public void testFetchObjectFactBindingsWithNonExistingObject() {
    assertEquals(0, getObjectManager().fetchObjectFactBindings(UUID.randomUUID()).size());
  }

  private ObjectTypeEntity createObjectType() {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName("objectType")
            .setValidator("validator")
            .setValidatorParameter("validatorParameter")
            .setEntityHandler("entityHandler")
            .setEntityHandlerParameter("entityHandlerParameter");
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

  private List<ObjectTypeEntity> createAndSaveObjectTypes(int numberOfEntities) {
    List<ObjectTypeEntity> entities = new ArrayList<>();

    for (int i = 0; i < numberOfEntities; i++) {
      ObjectTypeEntity entity = createObjectType();
      getObjectManager().saveObjectType(entity);
      entities.add(entity);
    }

    return entities;
  }

  private void assertObjectType(ObjectTypeEntity expected, ObjectTypeEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getNamespaceID(), actual.getNamespaceID());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getValidator(), actual.getValidator());
    assertEquals(expected.getValidatorParameter(), actual.getValidatorParameter());
    assertEquals(expected.getEntityHandler(), actual.getEntityHandler());
    assertEquals(expected.getEntityHandlerParameter(), actual.getEntityHandlerParameter());
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
