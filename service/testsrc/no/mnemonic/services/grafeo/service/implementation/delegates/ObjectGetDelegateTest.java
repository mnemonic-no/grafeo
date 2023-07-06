package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.ObjectTypeByIdResponseResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectGetDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactTypeByIdResponseResolver factTypeConverter;
  @Mock
  private ObjectTypeByIdResponseResolver objectTypeConverter;
  @Mock
  private ObjectTypeHandler objectTypeHandler;
  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private AccessControlCriteriaResolver accessControlCriteriaResolver;
  @Mock
  private IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  private ObjectGetDelegate delegate;

  @Before
  public void setup() throws Exception {
    initMocks(this);
    // Mocks required for ObjectConverter.
    when(accessControlCriteriaResolver.get()).thenReturn(AccessControlCriteria.builder()
            .addCurrentUserIdentity(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build());
    when(indexSelectCriteriaResolver.validateAndCreateCriteria(any(), any()))
            .thenReturn(IndexSelectCriteria.builder().build());
    when(objectFactDao.calculateObjectStatistics(any())).thenReturn(ObjectStatisticsContainer.builder().build());

    delegate = new ObjectGetDelegate(
            securityContext,
            accessControlCriteriaResolver,
            indexSelectCriteriaResolver,
            objectFactDao,
            factTypeConverter,
            objectTypeConverter,
            objectTypeHandler);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectByIdWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoFact);
    delegate.handle(new GetObjectByIdRequest());
  }

  @Test
  public void testFetchNonExistingObjectById() throws Exception {
    GetObjectByIdRequest request = new GetObjectByIdRequest().setId(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

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
    verify(securityContext).checkReadPermission(object);
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(Collections.singleton(object.getId()), criteria.getObjectID());
      assertNotNull(criteria.getAccessControlCriteria());
      assertNotNull(criteria.getIndexSelectCriteria());
      return true;
    }));
  }

  @Test
  public void testFetchObjectByIdIncludeTimeFilterInStatisticsCriteria() throws Exception {
    ObjectRecord object = new ObjectRecord().setId(UUID.randomUUID());
    GetObjectByIdRequest request = new GetObjectByIdRequest()
            .setId(object.getId())
            .setAfter(11111L)
            .setBefore(22222L);
    when(objectFactDao.getObject(object.getId())).thenReturn(object);

    assertNotNull(delegate.handle(request));
    verify(indexSelectCriteriaResolver).validateAndCreateCriteria(request.getAfter(), request.getBefore());
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(request.getAfter(), criteria.getStartTimestamp());
      assertEquals(request.getBefore(), criteria.getEndTimestamp());
      return true;
    }));
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectByTypeValueWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoFact);
    delegate.handle(new GetObjectByTypeValueRequest());
  }

  @Test
  public void testFetchObjectByTypeValueWithNonExistingObjectType() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");

    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeExists(request.getType(), "type");

    try {
      delegate.handle(request);
      fail();
    } catch (InvalidArgumentException ignored) {
      verifyNoInteractions(objectFactDao);
    }
  }

  @Test
  public void testFetchNonExistingObjectByTypeValue() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("type").setValue("value");
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

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

    when(objectFactDao.getObject(request.getType(), request.getValue())).thenReturn(object);

    assertNotNull(delegate.handle(request));
    verify(securityContext).checkReadPermission(object);
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(Collections.singleton(object.getId()), criteria.getObjectID());
      assertNotNull(criteria.getAccessControlCriteria());
      assertNotNull(criteria.getIndexSelectCriteria());
      return true;
    }));
  }

  @Test
  public void testFetchObjectByTypeValueIncludeTimeFilterInStatisticsCriteria() throws Exception {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest()
            .setType("type")
            .setValue("value")
            .setAfter(11111L)
            .setBefore(22222L);
    when(objectFactDao.getObject(request.getType(), request.getValue())).thenReturn(new ObjectRecord().setId(UUID.randomUUID()));

    assertNotNull(delegate.handle(request));
    verify(indexSelectCriteriaResolver).validateAndCreateCriteria(request.getAfter(), request.getBefore());
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(request.getAfter(), criteria.getStartTimestamp());
      assertEquals(request.getBefore(), criteria.getEndTimestamp());
      return true;
    }));
  }
}
