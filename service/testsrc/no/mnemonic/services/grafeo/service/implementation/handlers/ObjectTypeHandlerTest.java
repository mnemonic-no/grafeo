package no.mnemonic.services.grafeo.service.implementation.handlers;

import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeHandlerTest {

  @Mock
  private ObjectManager objectManager;
  @InjectMocks
  private ObjectTypeHandler handler;

  @Test
  public void testAssertObjectTypeExists() throws InvalidArgumentException {
    when(objectManager.getObjectType("someObjectType")).thenReturn(new ObjectTypeEntity());
    handler.assertObjectTypeExists("someObjectType", "type");
    verify(objectManager).getObjectType("someObjectType");
  }

  @Test
  public void testNonExistingObjectTypeThrowsException(){
    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> handler.assertObjectTypeExists("someObjectType", "type"));
    assertEquals(set("object.type.not.exist"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testAssertObjectTypeNotExists() throws InvalidArgumentException {
    handler.assertObjectTypeNotExists("someObjectType");
    verify(objectManager).getObjectType("someObjectType");
  }

  @Test
  public void testExistingObjectTypeThrowsException(){
    when(objectManager.getObjectType("someObjectType")).thenReturn(new ObjectTypeEntity());
    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> handler.assertObjectTypeNotExists("someObjectType"));
    assertEquals(set("object.type.exist"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }
}
