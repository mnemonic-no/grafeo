package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class ObjectTypeCreateDelegateTest extends AbstractDelegateTest {

  private ObjectTypeCreateDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new ObjectTypeCreateDelegate(getSecurityContext(), getObjectManager(), getObjectTypeConverter());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateObjectTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addTypes);
    delegate.handle(createRequest());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateAlreadyExistingObjectType() throws Exception {
    CreateObjectTypeRequest request = createRequest();
    when(getObjectManager().getObjectType(request.getName())).thenReturn(new ObjectTypeEntity());
    delegate.handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateObjectTypeValidatorNotFound() throws Exception {
    CreateObjectTypeRequest request = createRequest();
    when(getValidatorFactory().get(request.getValidator(), request.getValidatorParameter())).thenThrow(IllegalArgumentException.class);
    delegate.handle(request);
  }

  @Test
  public void testCreateObjectType() throws Exception {
    CreateObjectTypeRequest request = createRequest();
    ObjectTypeEntity newEntity = new ObjectTypeEntity();
    when(getObjectManager().saveObjectType(any())).thenReturn(newEntity);

    delegate.handle(request);
    verify(getObjectTypeConverter()).apply(newEntity);
    verify(getObjectManager()).saveObjectType(argThat(entity -> {
      assertNotNull(entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertEquals(request.getName(), entity.getName());
      assertEquals(request.getValidator(), entity.getValidator());
      assertEquals(request.getValidatorParameter(), entity.getValidatorParameter());
      return true;
    }));
  }

  private CreateObjectTypeRequest createRequest() {
    return new CreateObjectTypeRequest()
            .setName("ObjectType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter");
  }
}
