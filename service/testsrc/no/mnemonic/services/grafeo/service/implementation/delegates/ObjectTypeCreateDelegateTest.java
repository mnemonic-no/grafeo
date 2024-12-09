package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.handlers.ValidatorHandler;
import no.mnemonic.services.grafeo.service.validators.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeCreateDelegateTest {

  @Mock
  private ObjectTypeHandler objectTypeHandler;
  @Mock
  private ValidatorHandler validatorHandler;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private ObjectTypeResponseConverter objectTypeResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private ObjectTypeCreateDelegate delegate;

  @Test
  public void testCreateObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.addGrafeoType);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(createRequest()));
  }

  @Test
  public void testCreateAlreadyExistingObjectType() throws Exception {
    CreateObjectTypeRequest request = createRequest();
    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeNotExists(request.getName());
    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateObjectTypeValidatorNotFound() throws Exception {
    CreateObjectTypeRequest request = createRequest();
    doThrow(InvalidArgumentException.class)
            .when(validatorHandler).assertValidator(request.getValidator(), request.getValidatorParameter(), Validator.ApplicableType.ObjectType);
    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateObjectType() throws Exception {
    CreateObjectTypeRequest request = createRequest();
    ObjectTypeEntity newEntity = new ObjectTypeEntity();
    when(objectManager.saveObjectType(any())).thenReturn(newEntity);

    delegate.handle(request);
    verify(objectTypeResponseConverter).apply(newEntity);
    verify(objectManager).saveObjectType(argThat(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertEquals(request.getName(), entity.getName());
      assertEquals(request.getValidator(), entity.getValidator());
      assertEquals(request.getValidatorParameter(), entity.getValidatorParameter());
      assertTrue(entity.isSet(ObjectTypeEntity.Flag.TimeGlobalIndex));
      return true;
    }));
  }

  private CreateObjectTypeRequest createRequest() {
    return new CreateObjectTypeRequest()
            .setName("ObjectType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .setIndexOption(CreateObjectTypeRequest.IndexOption.TimeGlobal);
  }
}
