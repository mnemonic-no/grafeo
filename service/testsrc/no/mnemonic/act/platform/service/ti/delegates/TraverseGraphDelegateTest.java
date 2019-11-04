package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectSearchRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectTypeValueRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.converters.FactConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectConverter;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TraverseGraphDelegateTest extends AbstractDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectSearchDelegate objectSearch;
  @Mock
  private ObjectConverter objectConverter;
  @Mock
  private FactConverter factConverter;

  private TraverseGraphDelegate delegate;

  private final TestMethod byIdHandle = (object, query) -> {
    TraverseByObjectIdRequest request = new TraverseByObjectIdRequest()
            .setId(object.getId())
            .setQuery(query);
    ResultSet<?> result = delegate.handle(request);
    // Permission check should be performed on starting point of graph traversal.
    verify(getSecurityContext()).checkReadPermission(object);
    return result;
  };

  private final TestMethod byTypeValueHandle = (object, query) -> {
    TraverseByObjectTypeValueRequest request = new TraverseByObjectTypeValueRequest()
            .setType("objectType")
            .setValue(object.getValue())
            .setQuery(query);
    ResultSet<?> result = delegate.handle(request);
    // Permission check should be performed on starting point of graph traversal.
    verify(getSecurityContext()).checkReadPermission(object);
    return result;
  };

  private final TestMethod byObjectSearchHandle = (object, query) -> {
    TraverseByObjectSearchRequest request = new TraverseByObjectSearchRequest().setQuery(query);
    Set<Object> searchResult = Collections.singleton(Object.builder().setId(object.getId()).build());
    when(objectSearch.handle(request)).thenReturn(StreamingResultSet.<Object>builder().setValues(searchResult).build());

    return delegate.handle(request);
  };

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new TraverseGraphDelegate(
            getSecurityContext(),
            objectFactDao,
            getObjectManager(),
            getFactManager(),
            objectSearch,
            objectConverter,
            factConverter
    ).setScriptExecutionTimeout(2000);
  }

  @Test(expected = AccessDeniedException.class)
  public void testTraverseGraphByObjectIdWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.traverseFactObjects);
    delegate.handle(new TraverseByObjectIdRequest());
  }

  @Test
  public void testTraverseGraphByObjectIdWithoutObject() throws Exception {
    TraverseByObjectIdRequest request = new TraverseByObjectIdRequest().setId(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectRecord) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(objectFactDao).getObject(request.getId());
    }
  }

  @Test
  public void testTraverseGraphByObjectIdReturnEdges() throws Exception {
    testTraverseGraphReturnEdges(byIdHandle);
  }

  @Test
  public void testTraverseGraphByObjectIdReturnVertices() throws Exception {
    testTraverseGraphReturnVertices(byIdHandle);
  }

  @Test
  public void testTraverseGraphByObjectIdReturnValue() throws Exception {
    testTraverseGraphReturnValue(byIdHandle);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testTraverseGraphByObjectIdReturnError() throws Exception {
    testTraverseGraphReturnError(byIdHandle);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testTraverseGraphByObjectIdSandboxed() throws Exception {
    testTraverseGraphSandboxed(byIdHandle);
  }

  @Test(expected = OperationTimeoutException.class)
  public void testTraverseGraphByObjectIdTimeout() throws Exception {
    testTraverseGraphTimeout(byIdHandle);
  }

  @Test(expected = AccessDeniedException.class)
  public void testTraverseGraphByObjectTypeValueWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.traverseFactObjects);
    delegate.handle(new TraverseByObjectTypeValueRequest());
  }

  @Test
  public void testTraverseGraphByObjectTypeValueWithoutObjectType() throws Exception {
    TraverseByObjectTypeValueRequest request = new TraverseByObjectTypeValueRequest().setType("type").setValue("value");

    try {
      delegate.handle(request);
      fail();
    } catch (InvalidArgumentException ignored) {
      verify(getObjectManager()).getObjectType(request.getType());
    }
  }

  @Test
  public void testTraverseGraphByObjectTypeValueWithoutObject() throws Exception {
    TraverseByObjectTypeValueRequest request = new TraverseByObjectTypeValueRequest().setType("type").setValue("value");
    when(getObjectManager().getObjectType(request.getType())).thenReturn(new ObjectTypeEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectRecord) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObjectType(request.getType());
      verify(objectFactDao).getObject(request.getType(), request.getValue());
    }
  }

  @Test
  public void testTraverseGraphByObjectTypeValueReturnEdges() throws Exception {
    testTraverseGraphReturnEdges(byTypeValueHandle);
  }

  @Test
  public void testTraverseGraphByObjectTypeValueReturnVertices() throws Exception {
    testTraverseGraphReturnVertices(byTypeValueHandle);
  }

  @Test
  public void testTraverseGraphByObjectTypeValueReturnValue() throws Exception {
    testTraverseGraphReturnValue(byTypeValueHandle);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testTraverseGraphByObjectTypeValueReturnError() throws Exception {
    testTraverseGraphReturnError(byTypeValueHandle);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testTraverseGraphByObjectTypeValueSandboxed() throws Exception {
    testTraverseGraphSandboxed(byTypeValueHandle);
  }

  @Test(expected = OperationTimeoutException.class)
  public void testTraverseGraphByObjectTypeValueTimeout() throws Exception {
    testTraverseGraphTimeout(byTypeValueHandle);
  }

  @Test(expected = AccessDeniedException.class)
  public void testTraverseGraphByObjectSearchWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.traverseFactObjects);
    delegate.handle(new TraverseByObjectSearchRequest());
  }

  @Test
  public void testTraverseGraphByObjectSearchWithoutSearchResult() throws Exception {
    TraverseByObjectSearchRequest request = new TraverseByObjectSearchRequest();
    when(objectSearch.handle(request)).thenReturn(StreamingResultSet.<Object>builder().build());

    ResultSet<?> result = delegate.handle(request);
    assertFalse(result.iterator().hasNext());
  }

  @Test
  public void testTraverseGraphByObjectSearchReturnEdges() throws Exception {
    testTraverseGraphReturnEdges(byObjectSearchHandle);
  }

  @Test
  public void testTraverseGraphByObjectSearchReturnVertices() throws Exception {
    testTraverseGraphReturnVertices(byObjectSearchHandle);
  }

  @Test
  public void testTraverseGraphByObjectSearchReturnValue() throws Exception {
    testTraverseGraphReturnValue(byObjectSearchHandle);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testTraverseGraphByObjectSearchReturnError() throws Exception {
    testTraverseGraphReturnError(byObjectSearchHandle);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testTraverseGraphByObjectSearchSandboxed() throws Exception {
    testTraverseGraphSandboxed(byObjectSearchHandle);
  }

  @Test(expected = OperationTimeoutException.class)
  public void testTraverseGraphByObjectSearchTimeout() throws Exception {
    testTraverseGraphTimeout(byObjectSearchHandle);
  }

  private void testTraverseGraphReturnEdges(TestMethod method) throws Exception {
    ObjectRecord startObject = mockFullTraversal();
    List<?> result = ListUtils.list(method.execute(startObject, "g.outE()").iterator());
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof Fact);
    // SecurityContext should be called twice, once during graph traversal and once when creating result.
    verify(getSecurityContext()).hasReadPermission(isA(FactEntity.class));
    verify(getSecurityContext()).hasReadPermission(isA(FactRecord.class));
  }

  private void testTraverseGraphReturnVertices(TestMethod method) throws Exception {
    ObjectRecord startObject = mockFullTraversal();
    List<?> result = ListUtils.list(method.execute(startObject, "g.out()").iterator());
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof Object);
  }

  private void testTraverseGraphReturnValue(TestMethod method) throws Exception {
    ObjectRecord startObject = mockFullTraversal();
    List<?> result = ListUtils.list(method.execute(startObject, "g.values('value')").iterator());
    assertEquals(1, result.size());
    assertEquals(startObject.getValue(), result.get(0));
  }

  private void testTraverseGraphReturnError(TestMethod method) throws Exception {
    ObjectRecord startObject = mockFullTraversal();
    method.execute(startObject, "g.addE('notAllowed')");
  }

  private void testTraverseGraphSandboxed(TestMethod method) throws Exception {
    ObjectRecord startObject = mockFullTraversal();
    method.execute(startObject, "System.exit(0)");
  }

  private void testTraverseGraphTimeout(TestMethod method) throws Exception {
    ObjectRecord startObject = mockFullTraversal();
    method.execute(startObject, "while (true) {}");
  }

  private ObjectRecord mockFullTraversal() {
    when(getSecurityContext().hasReadPermission(isA(FactEntity.class))).thenReturn(true);
    when(getSecurityContext().hasReadPermission(isA(FactRecord.class))).thenReturn(true);

    ObjectRecord otherObject = mockFetchObject();
    FactEntity fact = mockFetchFact(otherObject);

    return mockFetchObject(fact);
  }

  private ObjectRecord mockFetchObject() {
    ObjectTypeEntity objectType = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("objectType");
    when(getObjectManager().getObjectType(objectType.getId())).thenReturn(objectType);
    when(getObjectManager().getObjectType(objectType.getName())).thenReturn(objectType);

    ObjectEntity entity = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(objectType.getId())
            .setValue("objectValue");
    ObjectRecord record = toRecord(entity);
    when(getObjectManager().getObject(entity.getId())).thenReturn(entity);
    when(getObjectManager().getObject(objectType.getName(), entity.getValue())).thenReturn(entity);
    when(objectFactDao.getObject(entity.getId())).thenReturn(record);
    when(objectFactDao.getObject(objectType.getName(), entity.getValue())).thenReturn(record);
    when(objectConverter.apply(record)).thenReturn(Object.builder().setId(entity.getId()).build());

    return record;
  }

  private ObjectRecord mockFetchObject(FactEntity fact) {
    ObjectRecord object = mockFetchObject();

    when(getObjectManager().fetchObjectFactBindings(object.getId())).thenReturn(ListUtils.list(
            new ObjectFactBindingEntity()
                    .setObjectID(object.getId())
                    .setFactID(fact.getId())
                    .setDirection(Direction.BiDirectional)
    ).iterator());

    return object;
  }

  private FactEntity mockFetchFact() {
    FactTypeEntity factType = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("factType");
    when(getFactManager().getFactType(factType.getId())).thenReturn(factType);

    FactEntity entity = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(factType.getId())
            .setValue("factValue");
    FactRecord record = toRecord(entity);
    when(getFactManager().getFact(entity.getId())).thenReturn(entity);
    when(objectFactDao.getFact(entity.getId())).thenReturn(record);
    when(factConverter.apply(record)).thenReturn(Fact.builder().setId(entity.getId()).build());

    return entity;
  }

  private FactEntity mockFetchFact(ObjectRecord object) {
    return mockFetchFact().setBindings(
            ListUtils.list(new FactEntity.FactObjectBinding()
                    .setObjectID(object.getId())
                    .setDirection(Direction.BiDirectional))
    );
  }

  private ObjectRecord toRecord(ObjectEntity entity) {
    return new ObjectRecord()
            .setId(entity.getId())
            .setTypeID(entity.getTypeID())
            .setValue(entity.getValue());
  }

  private FactRecord toRecord(FactEntity entity) {
    return new FactRecord()
            .setId(entity.getId())
            .setTypeID(entity.getTypeID())
            .setValue(entity.getValue());
  }

  private interface TestMethod {
    ResultSet<?> execute(ObjectRecord object, String query) throws Exception;
  }
}
