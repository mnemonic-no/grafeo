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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

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

  private ObjectTypeUpdateDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new ObjectTypeUpdateDelegate(
      securityContext,
      objectManager,
      objectTypeResponseConverter,
      objectTypeRequestResolver,
      objectTypeHandler);
  }

  @Test(expected = AccessDeniedException.class)
  public void testUpdateObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.updateThreatIntelType);
    delegate.handle(createRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testUpdateObjectTypeNotExisting() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    when(objectTypeRequestResolver.fetchExistingObjectType(request.getId())).thenThrow(ObjectNotFoundException.class);
    delegate.handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateObjectTypeWithExistingName() throws Exception {
    UpdateObjectTypeRequest request = createRequest();
    when(objectManager.getObjectType(request.getId())).thenReturn(new ObjectTypeEntity());
    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeNotExists(request.getName());
    delegate.handle(request);
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
