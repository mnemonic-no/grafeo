package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.converters.FactTypeByIdConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeByIdConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ObjectGetDelegateTest extends AbstractDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactTypeByIdConverter factTypeConverter;
  @Mock
  private ObjectTypeByIdConverter objectTypeConverter;

  private ObjectGetDelegate delegate;

  @Before
  public void setup() {
    // Mocks required for ObjectConverter.
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(objectFactDao.calculateObjectStatistics(any())).thenReturn(ObjectStatisticsContainer.builder().build());

    // initMocks() will be called by base class.
    delegate = new ObjectGetDelegate(
            getSecurityContext(),
            objectFactDao,
            factTypeConverter,
            objectTypeConverter
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectByIdWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    delegate.handle(new GetObjectByIdRequest());
  }

  @Test
  public void testFetchNonExistingObjectById() throws Exception {
    GetObjectByIdRequest request = new GetObjectByIdRequest().setId(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectRecord) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(objectFactDao).getObject(request.getId());
      verifyNoMoreInteractions(objectFactDao);
    }
  }

  @Test
  public void testFetchObjectById() throws Exception {
    ObjectRecord object = new ObjectRecord().setId(UUID.randomUUID());
    when(objectFactDao.getObject(object.getId())).thenReturn(object);

    assertNotNull(delegate.handle(new GetObjectByIdRequest().setId(object.getId())));
    verify(getSecurityContext()).checkReadPermission(object);
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(Collections.singleton(object.getId()), criteria.getObjectID());
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
      return true;
    }));
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectByTypeValueWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    delegate.handle(new GetObjectByTypeValueRequest());
  }

  @Test
  public void testFetchObjectByTypeValueWithNonExistingObjectType() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");

    try {
      delegate.handle(request);
      fail();
    } catch (InvalidArgumentException ignored) {
      verify(getObjectManager()).getObjectType(request.getType());
      verifyNoInteractions(objectFactDao);
    }
  }

  @Test
  public void testFetchNonExistingObjectByTypeValue() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");
    when(getObjectManager().getObjectType(request.getType())).thenReturn(new ObjectTypeEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectRecord) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(objectFactDao).getObject(request.getType(), request.getValue());
      verifyNoMoreInteractions(objectFactDao);
    }
  }

  @Test
  public void testFetchObjectByTypeValue() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");
    ObjectRecord object = new ObjectRecord().setId(UUID.randomUUID());
    when(getObjectManager().getObjectType(request.getType())).thenReturn(new ObjectTypeEntity());
    when(objectFactDao.getObject(request.getType(), request.getValue())).thenReturn(object);

    assertNotNull(delegate.handle(request));
    verify(getSecurityContext()).checkReadPermission(object);
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(Collections.singleton(object.getId()), criteria.getObjectID());
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
      return true;
    }));
  }
}
