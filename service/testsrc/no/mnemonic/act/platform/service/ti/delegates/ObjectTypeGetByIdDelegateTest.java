package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.ObjectTypeRequestResolver;
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
  private TiSecurityContext securityContext;

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
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelType);
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
