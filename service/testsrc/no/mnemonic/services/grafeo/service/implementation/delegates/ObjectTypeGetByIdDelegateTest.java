package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.ObjectTypeRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectTypeGetByIdDelegateTest {

  @Mock
  private ObjectTypeRequestResolver objectTypeRequestResolver;
  @Mock
  private ObjectTypeResponseConverter objectTypeResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;

  private ObjectTypeGetByIdDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new ObjectTypeGetByIdDelegate(
      securityContext,
      objectTypeResponseConverter,
      objectTypeRequestResolver);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewThreatIntelType);
    delegate.handle(new GetObjectTypeByIdRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchObjectTypeNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    when(objectTypeRequestResolver.fetchExistingObjectType(id)).thenThrow(ObjectNotFoundException.class);
    delegate.handle(new GetObjectTypeByIdRequest().setId(id));
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
