package no.mnemonic.services.grafeo.service.implementation.resolvers.request;

import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.providers.LockProvider;
import no.mnemonic.services.grafeo.service.validators.Validator;
import no.mnemonic.services.grafeo.service.validators.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectRequestResolverTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ValidatorFactory validatorFactory;
  @Mock
  private LockProvider lockProvider;

  private ObjectRequestResolver resolver;

  @Before
  public void initialize() {
    initMocks(this);
    resolver = new ObjectRequestResolver(objectManager, objectFactDao, validatorFactory, lockProvider);
  }

  @Test
  public void testResolveObjectWithInvalidInput() throws Exception {
    assertNull(resolver.resolveObject(null, "object"));
    assertNull(resolver.resolveObject("", "object"));
    assertNull(resolver.resolveObject("   ", "object"));
    assertNull(resolver.resolveObject("invalid", "object"));
  }

  @Test
  public void testResolveObjectById() throws Exception {
    UUID id = UUID.randomUUID();
    ObjectRecord object = new ObjectRecord();

    when(objectFactDao.getObject(id)).thenReturn(object);

    assertSame(object, resolver.resolveObject(id.toString(), "object"));

    verify(objectFactDao).getObject(id);
  }

  @Test
  public void testResolveObjectByTypeValue() throws Exception {
    String type = "ObjectType";
    String value = "ObjectValue";
    ObjectRecord object = new ObjectRecord();

    when(objectManager.getObjectType(type)).thenReturn(new ObjectTypeEntity());
    when(objectFactDao.getObject(type, value)).thenReturn(object);

    assertSame(object, resolver.resolveObject(String.format("%s/%s", type, value), "object"));

    verify(objectManager).getObjectType(type);
    verify(objectFactDao).getObject(type, value);
  }

  @Test
  public void testCreateMissingObject() throws Exception {
    String value = "ObjectValue";
    ObjectTypeEntity type = mockFetchObjectType();
    mockValidator(true);

    when(objectFactDao.storeObject(any())).thenAnswer(i -> i.getArgument(0));

    String requestedObject = String.format("%s/%s", type.getName(), value);
    ObjectRecord resolvedObject = resolver.resolveObject(requestedObject, "object");
    assertObjectRecord(resolvedObject, type.getId(), value);

    verify(objectFactDao).storeObject(argThat(record -> assertObjectRecord(record, type.getId(), value)));
    verify(lockProvider).acquireLock("ObjectRequestResolver", requestedObject);
  }

  @Test
  public void testCreateMissingObjectFailsOnMissingObjectType() {
    try {
      resolver.resolveObject("ObjectType/ObjectValue", "object");
      fail();
    } catch (InvalidArgumentException ex) {
      assertEquals("object.type.not.exist", ex.getValidationErrors().iterator().next().getMessageTemplate());
      verify(objectFactDao, never()).storeObject(any());
    }
  }

  @Test
  public void testCreateMissingObjectFailsOnObjectValidation() {
    ObjectTypeEntity type = mockFetchObjectType();
    mockValidator(false);

    try {
      resolver.resolveObject(String.format("%s/%s", type.getName(), "ObjectValue"), "object");
      fail();
    } catch (InvalidArgumentException ex) {
      assertEquals("object.not.valid", ex.getValidationErrors().iterator().next().getMessageTemplate());
      verify(objectFactDao, never()).storeObject(any());
    }
  }

  private void mockValidator(boolean valid) {
    Validator validator = mock(Validator.class);
    when(validator.validate(anyString())).thenReturn(valid);
    when(validatorFactory.get(anyString(), anyString())).thenReturn(validator);
  }

  private ObjectTypeEntity mockFetchObjectType() {
    ObjectTypeEntity type = new ObjectTypeEntity()
      .setId(UUID.randomUUID())
      .setName("ObjectType")
      .setValidator("Validator")
      .setValidatorParameter("Parameter");
    when(objectManager.getObjectType(type.getName())).thenReturn(type);
    return type;
  }

  private boolean assertObjectRecord(ObjectRecord object, UUID type, String value) {
    assertNotNull(object.getId());
    assertEquals(type, object.getTypeID());
    assertEquals(value, object.getValue());
    return true;
  }

}
