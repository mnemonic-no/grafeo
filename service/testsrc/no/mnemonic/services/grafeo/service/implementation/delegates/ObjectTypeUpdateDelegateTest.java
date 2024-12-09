package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.ObjectTypeRequestResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeUpdateDelegateTest {

  @Mock
  private ObjectTypeRequestResolver objectTypeRequestResolver;
  @Mock
  private ObjectTypeHandler objectTypeHandler;
  @Mock
  private ObjectManager objectManager;
  @Mock
  private ObjectTypeResponseConverter objectTypeResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private ObjectTypeUpdateDelegate delegate;

  @Test
  public void testUpdateObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.updateGrafeoType);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(createRequest()));
  }

  @Test
  public void testUpdateObjectTypeNotExisting() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    when(objectTypeRequestResolver.fetchExistingObjectType(request.getId())).thenThrow(ObjectNotFoundException.class);
    assertThrows(ObjectNotFoundException.class, () -> delegate.handle(request));
  }

  @Test
  public void testUpdateObjectTypeWithExistingName() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeNotExists(request.getName());
    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testUpdateObjectType() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    ObjectTypeEntity entity = new ObjectTypeEntity();
    when(objectTypeRequestResolver.fetchExistingObjectType(request.getId())).thenReturn(entity);
    when(objectManager.saveObjectType(argThat(e -> {
      assertSame(entity, e);
      assertEquals(request.getName(), e.getName());
      return true;
    }))).thenReturn(entity);

    delegate.handle(request);
    verify(objectTypeResponseConverter).apply(entity);
  }

  private UpdateObjectTypeRequest createRequest() {
    return new UpdateObjectTypeRequest()
            .setId(UUID.randomUUID())
            .setName("newName");
  }
}
