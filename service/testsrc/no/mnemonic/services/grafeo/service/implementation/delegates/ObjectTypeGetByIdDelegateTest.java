package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.ObjectTypeRequestResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeGetByIdDelegateTest {

  @Mock
  private ObjectTypeRequestResolver objectTypeRequestResolver;
  @Mock
  private ObjectTypeResponseConverter objectTypeResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private ObjectTypeGetByIdDelegate delegate;

  @Test
  public void testFetchObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoType);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new GetObjectTypeByIdRequest()));
  }

  @Test
  public void testFetchObjectTypeNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    when(objectTypeRequestResolver.fetchExistingObjectType(id)).thenThrow(ObjectNotFoundException.class);
    assertThrows(ObjectNotFoundException.class, () -> delegate.handle(new GetObjectTypeByIdRequest().setId(id)));
  }

  @Test
  public void testFetchObjectType() throws Exception {
    UUID id = UUID.randomUUID();
    ObjectTypeEntity entity = new ObjectTypeEntity();

    when(objectTypeRequestResolver.fetchExistingObjectType(id)).thenReturn(entity);
    delegate.handle(new GetObjectTypeByIdRequest().setId(id));
    verify(objectTypeResponseConverter).apply(entity);
  }
}
