package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsResult;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ObjectGetDelegateTest extends AbstractDelegateTest {

  @Before
  public void setup() {
    // Mocks required for ObjectConverter.
    when(getObjectTypeConverter().apply(any())).thenReturn(ObjectType.builder().build());
    when(getFactTypeConverter().apply(any())).thenReturn(FactType.builder().build());
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(getFactSearchManager().calculateObjectStatistics(any())).thenReturn(ObjectStatisticsResult.builder().build());
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectByIdWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectGetDelegate.create().handle(new GetObjectByIdRequest());
  }

  @Test
  public void testFetchNonExistingObjectById() throws Exception {
    GetObjectByIdRequest request = new GetObjectByIdRequest().setId(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

    try {
      ObjectGetDelegate.create().handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObject(request.getId());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testFetchObjectById() throws Exception {
    ObjectEntity object = new ObjectEntity().setId(UUID.randomUUID());
    when(getObjectManager().getObject(object.getId())).thenReturn(object);

    assertNotNull(ObjectGetDelegate.create().handle(new GetObjectByIdRequest().setId(object.getId())));
    verify(getSecurityContext()).checkReadPermission(object);
    verify(getFactSearchManager()).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(Collections.singleton(object.getId()), criteria.getObjectID());
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
      return true;
    }));
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectByTypeValueWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectGetDelegate.create().handle(new GetObjectByTypeValueRequest());
  }

  @Test
  public void testFetchObjectByTypeValueWithNonExistingObjectType() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");

    try {
      ObjectGetDelegate.create().handle(request);
      fail();
    } catch (InvalidArgumentException ignored) {
      verify(getObjectManager()).getObjectType(request.getType());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testFetchNonExistingObjectByTypeValue() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");
    when(getObjectManager().getObjectType(request.getType())).thenReturn(new ObjectTypeEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

    try {
      ObjectGetDelegate.create().handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObjectType(request.getType());
      verify(getObjectManager()).getObject(request.getType(), request.getValue());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testFetchObjectByTypeValue() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");
    ObjectEntity object = new ObjectEntity().setId(UUID.randomUUID());
    when(getObjectManager().getObjectType(request.getType())).thenReturn(new ObjectTypeEntity());
    when(getObjectManager().getObject(request.getType(), request.getValue())).thenReturn(object);

    assertNotNull(ObjectGetDelegate.create().handle(request));
    verify(getSecurityContext()).checkReadPermission(object);
    verify(getFactSearchManager()).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(Collections.singleton(object.getId()), criteria.getObjectID());
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
      return true;
    }));
  }

}
