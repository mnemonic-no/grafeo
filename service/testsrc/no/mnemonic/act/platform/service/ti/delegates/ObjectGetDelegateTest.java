package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ObjectGetDelegateTest extends AbstractDelegateTest {

  private final TestMethod byIdHandle = object -> {
    GetObjectByIdRequest request = new GetObjectByIdRequest().setId(object.getId());
    return ObjectGetDelegate.create().handle(request);
  };

  private final TestMethod byTypeValueHandle = object -> {
    GetObjectByTypeValueRequest request = new GetObjectByTypeValueRequest().setType("objectType").setValue(object.getValue());
    return ObjectGetDelegate.create().handle(request);
  };

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectByIdWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectGetDelegate.create().handle(new GetObjectByIdRequest());
  }

  @Test
  public void testFetchNonExistingObjectById() throws Exception {
    GetObjectByIdRequest request = new GetObjectByIdRequest().setId(UUID.randomUUID());

    try {
      ObjectGetDelegate.create().handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObject(request.getId());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testFetchObjectByIdWithoutFacts() throws Exception {
    testFetchObjectWithoutFacts(byIdHandle);
  }

  @Test
  public void testFetchObjectByIdWithoutAccessToFacts() throws Exception {
    testFetchObjectWithoutAccessToFacts(byIdHandle);
  }

  @Test
  public void testFetchObjectByIdIncludeStatistics() throws Exception {
    testFetchObjectIncludeStatistics(byIdHandle);
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
  public void testFetchObjectByTypeValueWithoutFacts() throws Exception {
    testFetchObjectWithoutFacts(byTypeValueHandle);
  }

  @Test
  public void testFetchObjectByTypeValueWithoutAccessToFacts() throws Exception {
    testFetchObjectWithoutAccessToFacts(byTypeValueHandle);
  }

  @Test
  public void testFetchObjectByTypeValueIncludeStatistics() throws Exception {
    testFetchObjectIncludeStatistics(byTypeValueHandle);
  }

  private ObjectEntity mockFetchObject() {
    ObjectTypeEntity objectType = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("objectType");
    when(getObjectManager().getObjectType(objectType.getId())).thenReturn(objectType);
    when(getObjectManager().getObjectType(objectType.getName())).thenReturn(objectType);
    when(getObjectTypeConverter().apply(objectType)).thenReturn(ObjectType.builder().setName(objectType.getName()).build());

    ObjectEntity object = new ObjectEntity()
            .setId(UUID.randomUUID())
            .setTypeID(objectType.getId())
            .setValue("objectValue");
    when(getObjectManager().getObject(object.getId())).thenReturn(object);
    when(getObjectManager().getObject(objectType.getName(), object.getValue())).thenReturn(object);

    return object;
  }

  private List<FactEntity> mockFetchFacts() {
    FactTypeEntity factType = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("factType");
    when(getFactManager().getFactType(factType.getId())).thenReturn(factType);
    when(getFactManager().getFactType(factType.getName())).thenReturn(factType);
    when(getFactTypeConverter().apply(factType)).thenReturn(FactType.builder().setName(factType.getName()).build());

    List<FactEntity> facts = new ArrayList<>();
    facts.add(mockFetchFact(factType, 1111, 6666));
    facts.add(mockFetchFact(factType, 3333, 4444));
    facts.add(mockFetchFact(factType, 2222, 5555));

    return facts;
  }

  private FactEntity mockFetchFact(FactTypeEntity factType, long timestamp, long lastSeenTimestamp) {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(factType.getId())
            .setValue("factValue")
            .setAccessMode(AccessMode.Public)
            .setTimestamp(timestamp)
            .setLastSeenTimestamp(lastSeenTimestamp);

    when(getFactManager().getFact(fact.getId())).thenReturn(fact);

    return fact;
  }

  private void mockBindings(UUID objectID, List<FactEntity> facts) {
    List<ObjectFactBindingEntity> bindings = facts.stream()
            .map(fact -> new ObjectFactBindingEntity().setObjectID(objectID).setFactID(fact.getId()))
            .collect(Collectors.toList());

    when(getObjectManager().fetchObjectFactBindings(objectID)).thenReturn(bindings);
  }

  private void testFetchObjectWithoutFacts(TestMethod method) throws Exception {
    ObjectEntity object = mockFetchObject();

    try {
      method.execute(object);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).fetchObjectFactBindings(object.getId());
      verify(getFactManager(), never()).getFact(any());
    }
  }

  private void testFetchObjectWithoutAccessToFacts(TestMethod method) throws Exception {
    ObjectEntity object = mockFetchObject();
    List<FactEntity> facts = mockFetchFacts();
    mockBindings(object.getId(), facts);

    try {
      method.execute(object);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).fetchObjectFactBindings(object.getId());
      verify(getFactManager(), times(facts.size())).getFact(any());
      verify(getSecurityContext(), times(facts.size())).hasReadPermission(any());
    }
  }

  private void testFetchObjectIncludeStatistics(TestMethod method) throws Exception {
    when(getSecurityContext().hasReadPermission(any())).thenReturn(true);

    ObjectEntity object = mockFetchObject();
    mockBindings(object.getId(), mockFetchFacts());

    Object result = method.execute(object);

    assertEquals(object.getId(), result.getId());
    assertEquals("objectType", result.getType().getName());
    assertEquals(object.getValue(), result.getValue());
    assertEquals(1, result.getStatistics().size());
    assertEquals("factType", result.getStatistics().get(0).getType().getName());
    assertEquals(3, result.getStatistics().get(0).getCount());
    assertEquals(3333, (long) result.getStatistics().get(0).getLastAddedTimestamp());
    assertEquals(6666, (long) result.getStatistics().get(0).getLastSeenTimestamp());
  }

  private interface TestMethod {
    Object execute(ObjectEntity object) throws AccessDeniedException, AuthenticationFailedException, ObjectNotFoundException, InvalidArgumentException;
  }

}
