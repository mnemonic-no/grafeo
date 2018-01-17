package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectResolverTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private ValidatorFactory validatorFactory;

  private ObjectResolver resolver;

  @Before
  public void initialize() {
    initMocks(this);
    resolver = new ObjectResolver(objectManager, validatorFactory);
  }

  @Test
  public void testResolveObjectById() throws Exception {
    UUID id = UUID.randomUUID();
    ObjectEntity object = new ObjectEntity();

    when(objectManager.getObject(id)).thenReturn(object);

    assertSame(object, resolver.resolveObject(id, "", ""));

    verify(objectManager).getObject(id);
    verifyNoMoreInteractions(objectManager);
  }

  @Test
  public void testResolveObjectByTypeValue() throws Exception {
    String type = "ObjectType";
    String value = "ObjectValue";
    ObjectEntity object = new ObjectEntity();

    when(objectManager.getObject(type, value)).thenReturn(object);

    assertSame(object, resolver.resolveObject(null, type, value));

    verify(objectManager).getObject(null);
    verify(objectManager).getObject(type, value);
    verifyNoMoreInteractions(objectManager);
  }

  @Test
  public void testCreateMissingObject() throws Exception {
    String value = "ObjectValue";
    ObjectTypeEntity type = mockFetchObjectType();
    mockValidator(true);

    when(objectManager.saveObject(any())).thenAnswer(i -> i.getArgument(0));

    ObjectEntity resolvedObject = resolver.resolveObject(null, type.getName(), value);
    assertObjectEntity(resolvedObject, type.getId(), value);

    verify(objectManager).saveObject(argThat(e -> {
      assertObjectEntity(e, type.getId(), value);
      return true;
    }));
  }

  @Test
  public void testCreateMissingObjectFailsOnMissingObjectType() throws Exception {
    try {
      resolver.resolveObject(null, "ObjectType", "ObjectValue");
      fail();
    } catch (InvalidArgumentException ex) {
      assertEquals("object.type.not.exist", ex.getValidationErrors().iterator().next().getMessageTemplate());
      verify(objectManager, never()).saveObject(any());
    }
  }

  @Test
  public void testCreateMissingObjectFailsOnObjectValidation() throws Exception {
    ObjectTypeEntity type = mockFetchObjectType();
    mockValidator(false);

    try {
      resolver.resolveObject(null, type.getName(), "ObjectValue");
      fail();
    } catch (InvalidArgumentException ex) {
      assertEquals("object.not.valid", ex.getValidationErrors().iterator().next().getMessageTemplate());
      verify(objectManager, never()).saveObject(any());
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

  private void assertObjectEntity(ObjectEntity object, UUID type, String value) {
    assertNotNull(object.getId());
    assertEquals(type, object.getTypeID());
    assertEquals(value, object.getValue());
  }

}
