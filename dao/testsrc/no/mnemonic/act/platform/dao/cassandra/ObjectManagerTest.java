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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ObjectManagerTest extends AbstractManagerTest {

  @Test
  public void testSaveAndGetObjectTypeById() {
    ObjectTypeEntity entity = createAndSaveObjectType();
    assertObjectType(entity, getObjectManager().getObjectType(entity.getId()));
  }

  @Test
  public void testGetObjectTypeWithUnknownIdReturnsNull() {
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

    Comparator<ObjectTypeEntity> comparator = (e1, e2) -> e1.getId().compareTo(e2.getId());
    expected.sort(comparator);
    actual.sort(comparator);

    assertObjectTypes(expected, actual);
  }

  @Test
  public void testSaveAndGetObject() throws ImmutableViolationException {
    ObjectEntity object = createAndSaveObject(createAndSaveObjectType().getId());
    assertObject(object, getObjectManager().getObject(object.getId()));
  }

  @Test
  public void testGetObjectWithNonExistingObject() throws ImmutableViolationException {
    assertNull(getObjectManager().getObject(UUID.randomUUID()));
  }

  @Test
  public void testSaveAndGetObjectByTypeValue() throws ImmutableViolationException {
    ObjectTypeEntity type = createAndSaveObjectType();
    ObjectEntity object = createAndSaveObject(type.getId());
    assertObject(object, getObjectManager().getObject(type.getName(), object.getValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetObjectByTypeValueWithNonExistingObjectType() throws ImmutableViolationException {
    ObjectEntity entity = createAndSaveObject(createAndSaveObjectType().getId());
    getObjectManager().getObject("nonExisting", entity.getValue());
  }

  @Test
  public void testGetObjectByTypeValueWithNonExistingObjectValue() throws ImmutableViolationException {
    ObjectTypeEntity entity = createAndSaveObjectType();
    assertNull(getObjectManager().getObject(entity.getName(), "nonExisting"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveObjectWithNonExistingObjectType() throws ImmutableViolationException {
    getObjectManager().saveObject(createObject());
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveSameObjectTwice() throws ImmutableViolationException {
    ObjectTypeEntity type = createAndSaveObjectType();
    ObjectEntity object = createObject(type.getId());
    getObjectManager().saveObject(object);
    getObjectManager().saveObject(object);
  }

  @Test
  public void testSaveObjectReturnsSameEntity() throws ImmutableViolationException {
    ObjectEntity entity = createObject(createAndSaveObjectType().getId());
    assertSame(entity, getObjectManager().saveObject(entity));
  }

  @Test
  public void testSaveObjectReturnsNullOnNullInput() throws ImmutableViolationException {
    assertNull(getObjectManager().saveObject(null));
  }

  @Test
  public void testEncodeWhenSavingObject() throws ImmutableViolationException {
    createAndSaveObject(createAndSaveObjectType().getId());

    // Once for saving Object and once for checking for existing Object.
    verify(getEntityHandler(), times(2)).encode(any());
  }

  @Test
  public void testDecodeWhenRetrievingObject() throws ImmutableViolationException {
    ObjectTypeEntity type = createAndSaveObjectType();
    ObjectEntity object = createAndSaveObject(type.getId());

    getObjectManager().getObject(object.getId());
    getObjectManager().getObject(type.getName(), object.getValue());

    // Once for each getObject().
    verify(getEntityHandler(), times(2)).decode(any());
  }

  @Test
  public void testSaveAndFetchObjectFactBindings() throws ImmutableViolationException {
    ObjectEntity object = createAndSaveObject(createAndSaveObjectType().getId());
    ObjectFactBindingEntity binding = createAndSaveObjectFactBinding(object.getId());

    List<ObjectFactBindingEntity> actual = getObjectManager().fetchObjectFactBindings(object.getId());
    assertEquals(1, actual.size());
    assertObjectFactBinding(binding, actual.get(0));
  }

  @Test
  public void testFetchObjectFactBindingsWithNonExistingObject() {
    assertEquals(0, getObjectManager().fetchObjectFactBindings(UUID.randomUUID()).size());
  }

  @Test
  public void testSaveObjectFactBindingReturnsSameEntity() throws ImmutableViolationException {
    ObjectFactBindingEntity binding = createObjectFactBinding(createAndSaveObject().getId());
    assertSame(binding, getObjectManager().saveObjectFactBinding(binding));
  }

  @Test
  public void testSaveObjectFactBindingReturnsNullOnNullInput() throws ImmutableViolationException {
    assertNull(getObjectManager().saveObjectFactBinding(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveObjectFactBindingWithNonExistingObjectThrowsException() throws ImmutableViolationException {
    getObjectManager().saveObjectFactBinding(createObjectFactBinding(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveObjectFactBindingTwiceThrowsException() throws ImmutableViolationException {
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

  private ObjectEntity createAndSaveObject() throws ImmutableViolationException {
    return createAndSaveObject(createAndSaveObjectType().getId());
  }

  private ObjectEntity createAndSaveObject(UUID typeID) throws ImmutableViolationException {
    return getObjectManager().saveObject(createObject(typeID));
  }

  private ObjectFactBindingEntity createAndSaveObjectFactBinding(UUID objectID) throws ImmutableViolationException {
    return getObjectManager().saveObjectFactBinding(createObjectFactBinding(objectID));
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
