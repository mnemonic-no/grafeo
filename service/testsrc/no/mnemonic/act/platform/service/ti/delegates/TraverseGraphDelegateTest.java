package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectSearchRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectTypeValueRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TraverseGraphDelegateTest extends AbstractDelegateTest {

  @Mock
  private ObjectSearchDelegate objectSearch;

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
    when(objectSearch.handle(request)).thenReturn(ResultSet.<Object>builder().setValues(searchResult).build());

    return delegate.handle(request);
  };

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = TraverseGraphDelegate.builder()
            .setObjectSearch(objectSearch)
            .setObjectConverter(getObjectConverter())
            .setFactConverter(getFactConverter())
            .setScriptExecutionTimeout(2000)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutObjectSearch() {
    TraverseGraphDelegate.builder()
            .setObjectConverter(getObjectConverter())
            .setFactConverter(getFactConverter())
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutObjectConverter() {
    TraverseGraphDelegate.builder()
            .setObjectSearch(objectSearch)
            .setFactConverter(getFactConverter())
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactConverter() {
    TraverseGraphDelegate.builder()
            .setObjectSearch(objectSearch)
            .setObjectConverter(getObjectConverter())
            .build();
  }

  @Test(expected = AccessDeniedException.class)
  public void testTraverseGraphByObjectIdWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.traverseFactObjects);
    delegate.handle(new TraverseByObjectIdRequest());
  }

  @Test
  public void testTraverseGraphByObjectIdWithoutObject() throws Exception {
    TraverseByObjectIdRequest request = new TraverseByObjectIdRequest().setId(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObject(request.getId());
      verifyNoMoreInteractions(getObjectManager());
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
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testTraverseGraphByObjectTypeValueWithoutObject() throws Exception {
    TraverseByObjectTypeValueRequest request = new TraverseByObjectTypeValueRequest().setType("type").setValue("value");
    when(getObjectManager().getObjectType(request.getType())).thenReturn(new ObjectTypeEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObjectType(request.getType());
      verify(getObjectManager()).getObject(request.getType(), request.getValue());
      verifyNoMoreInteractions(getObjectManager());
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
    when(objectSearch.handle(request)).thenReturn(ResultSet.<Object>builder().build());

    ResultSet<?> result = delegate.handle(request);
    assertTrue(result.getValues().isEmpty());
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
    ObjectEntity startObject = mockFullTraversal();
    ResultSet<?> result = method.execute(startObject, "g.outE()");
    assertEquals(1, result.getValues().size());
    assertTrue(result.getValues().iterator().next() instanceof Fact);
    // SecurityContext should be called twice, once during graph traversal and once when creating result.
    verify(getSecurityContext(), times(2)).hasReadPermission(isA(FactEntity.class));
  }

  private void testTraverseGraphReturnVertices(TestMethod method) throws Exception {
    ObjectEntity startObject = mockFullTraversal();
    ResultSet<?> result = method.execute(startObject, "g.out()");
    assertEquals(1, result.getValues().size());
    assertTrue(result.getValues().iterator().next() instanceof Object);
  }

  private void testTraverseGraphReturnValue(TestMethod method) throws Exception {
    ObjectEntity startObject = mockFullTraversal();
    ResultSet<?> result = method.execute(startObject, "g.values('value')");
    assertEquals(1, result.getValues().size());
    assertEquals(startObject.getValue(), result.getValues().iterator().next());
  }

  private void testTraverseGraphReturnError(TestMethod method) throws Exception {
    ObjectEntity startObject = mockFullTraversal();
    method.execute(startObject, "g.addE('notAllowed')");
  }

  private void testTraverseGraphSandboxed(TestMethod method) throws Exception {
    ObjectEntity startObject = mockFullTraversal();
    method.execute(startObject, "System.exit(0)");
  }

  private void testTraverseGraphTimeout(TestMethod method) throws Exception {
    ObjectEntity startObject = mockFullTraversal();
    method.execute(startObject, "while (true) {}");
  }

  private ObjectEntity mockFullTraversal() {
    when(getSecurityContext().hasReadPermission(isA(FactEntity.class))).thenReturn(true);

    ObjectEntity otherObject = mockFetchObject();
    FactEntity fact = mockFetchFact(otherObject);

    return mockFetchObject(fact);
  }

  private ObjectEntity mockFetchObject() {
    ObjectTypeEntity objectType = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("objectType");
    when(getObjectManager().getObjectType(objectType.getId())).thenReturn(objectType);
    when(getObjectManager().getObjectType(objectType.getName())).thenReturn(objectType);

    ObjectEntity object = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(objectType.getId())
            .setValue("objectValue");
    when(getObjectManager().getObject(object.getId())).thenReturn(object);
    when(getObjectManager().getObject(objectType.getName(), object.getValue())).thenReturn(object);
    when(getObjectConverter().apply(object)).thenReturn(Object.builder().setId(object.getId()).build());

    return object;
  }

  private ObjectEntity mockFetchObject(FactEntity fact) {
    ObjectEntity object = mockFetchObject();

    when(getObjectManager().fetchObjectFactBindings(object.getId())).thenReturn(ListUtils.list(
            new ObjectFactBindingEntity()
                    .setObjectID(object.getId())
                    .setFactID(fact.getId())
                    .setDirection(Direction.BiDirectional)
    ));

    return object;
  }

  private FactEntity mockFetchFact() {
    FactTypeEntity factType = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("factType");
    when(getFactManager().getFactType(factType.getId())).thenReturn(factType);

    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(factType.getId())
            .setValue("factValue");
    when(getFactManager().getFact(fact.getId())).thenReturn(fact);
    when(getFactConverter().apply(any())).thenReturn(Fact.builder().setId(fact.getId()).build());

    return fact;
  }

  private FactEntity mockFetchFact(ObjectEntity object) {
    return mockFetchFact().setBindings(
            ListUtils.list(new FactEntity.FactObjectBinding()
                    .setObjectID(object.getId())
                    .setDirection(Direction.BiDirectional))
    );
  }

  private interface TestMethod {
    ResultSet<?> execute(ObjectEntity object, String query) throws Exception;
  }

}
