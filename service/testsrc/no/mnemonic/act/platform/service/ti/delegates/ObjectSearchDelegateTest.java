package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ObjectSearchDelegateTest extends AbstractDelegateTest {

  private ObjectTypeEntity objectType1 = new ObjectTypeEntity()
          .setId(UUID.randomUUID())
          .setName("objectType1");
  private ObjectTypeEntity objectType2 = new ObjectTypeEntity()
          .setId(UUID.randomUUID())
          .setName("objectType2");
  private ObjectEntity object1 = new ObjectEntity()
          .setId(UUID.randomUUID())
          .setTypeID(objectType1.getId())
          .setValue("aaa");
  private ObjectEntity object2 = new ObjectEntity()
          .setId(UUID.randomUUID())
          .setTypeID(objectType1.getId())
          .setValue("bbb");
  private ObjectEntity object3 = new ObjectEntity()
          .setId(UUID.randomUUID())
          .setTypeID(objectType2.getId())
          .setValue("ccc");
  private FactTypeEntity factType1 = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("ip");
  private FactTypeEntity factType2 = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("domain");
  private FactEntity fact1 = new FactEntity()
          .setId(UUID.randomUUID())
          .setTypeID(factType1.getId())
          .setValue("1.1.1.1")
          .setSourceID(UUID.randomUUID())
          .setTimestamp(1111);
  private FactEntity fact2 = new FactEntity()
          .setId(UUID.randomUUID())
          .setTypeID(factType1.getId())
          .setValue("2.2.2.2")
          .setSourceID(UUID.randomUUID())
          .setTimestamp(2222);
  private FactEntity fact3 = new FactEntity()
          .setId(UUID.randomUUID())
          .setTypeID(factType2.getId())
          .setValue("example.org")
          .setSourceID(UUID.randomUUID())
          .setTimestamp(3333);
  private List<ObjectEntity> objects = ListUtils.list(object1, object2, object3);
  private List<FactEntity> facts = ListUtils.list(fact1, fact2, fact3);

  @Before
  public void setup() {
    mockFetchObjects();
    mockFetchFacts();
    mockFetchBindings();
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchObjectsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectSearchDelegate.create().handle(new SearchObjectRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchObjectsForbidUnlimited() throws Exception {
    ObjectSearchDelegate.create().handle(new SearchObjectRequest().setLimit(0));
  }

  @Test
  public void testSearchObjectsLimitedResult() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().setLimit(1);
    ResultSet<Object> result = ObjectSearchDelegate.create().handle(request);

    assertEquals((long) request.getLimit(), result.getLimit());
    assertEquals((long) request.getLimit(), result.getCount());
    assertEquals((long) request.getLimit(), result.getValues().size());
  }

  @Test
  public void testSearchObjectsWithDefaultLimit() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest();
    ResultSet<Object> result = ObjectSearchDelegate.create().handle(request);

    assertEquals(25, result.getLimit());
    assertEquals(objects.size(), result.getCount());
    assertEquals(objects.size(), result.getValues().size());
  }

  @Test
  public void testSearchObjectsFilterByObjectType() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().addObjectType(objectType2.getName());
    assertResult(object3, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjectsFilterByObjectValue() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().addObjectValue(object2.getValue());
    assertResult(object2, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjectsFilterByFactType() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().addFactType(factType2.getName());
    assertResult(object3, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjectsFilterByFactValue() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().addFactValue(fact2.getValue());
    assertResult(object2, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjectsFilterBySource() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().addSource(fact1.getSourceID().toString());
    assertResult(object1, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjectsFilterByBefore() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().setBefore(2000L);
    assertResult(object1, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjectsFilterByAfter() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest().setAfter(3000L);
    assertResult(object3, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjectsCombineMultipleFilters() throws Exception {
    SearchObjectRequest request = new SearchObjectRequest()
            .addObjectType(objectType1.getName())
            .addObjectValue(object1.getValue())
            .addFactType(factType1.getName())
            .addFactValue(fact1.getValue())
            .addSource(fact1.getSourceID().toString())
            .setBefore(2000L)
            .setAfter(1000L);
    assertResult(object1, ObjectSearchDelegate.create().handle(request));
  }

  @Test
  public void testSearchObjects() throws Exception {
    ObjectSearchDelegate.create().handle(new SearchObjectRequest());

    verify(getObjectManager()).fetchObjects();
    verify(getObjectManager(), times(objects.size())).fetchObjectFactBindings(any());
    verify(getObjectConverter(), times(objects.size())).apply(any());
    verify(getFactManager(), times(facts.size())).getFact(any());
  }

  private void assertResult(ObjectEntity object, ResultSet<Object> result) {
    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(object.getId(), result.getValues().iterator().next().getId());
  }

  private void mockFetchObjects() {
    when(getObjectManager().getObjectType(objectType1.getId())).thenReturn(objectType1);
    when(getObjectManager().getObjectType(objectType1.getName())).thenReturn(objectType1);
    when(getObjectManager().getObjectType(objectType2.getId())).thenReturn(objectType2);
    when(getObjectManager().getObjectType(objectType2.getName())).thenReturn(objectType2);

    when(getObjectManager().fetchObjects()).thenReturn(objects.iterator());

    when(getObjectConverter().apply(any())).thenAnswer(i -> {
      ObjectEntity entity = i.getArgument(0);
      return Object.builder().setId(entity.getId()).build();
    });
  }

  private void mockFetchFacts() {
    when(getFactManager().getFactType(factType1.getId())).thenReturn(factType1);
    when(getFactManager().getFactType(factType1.getName())).thenReturn(factType1);
    when(getFactManager().getFactType(factType2.getId())).thenReturn(factType2);
    when(getFactManager().getFactType(factType2.getName())).thenReturn(factType2);

    for (FactEntity fact : facts) {
      when(getFactManager().getFact(fact.getId())).thenReturn(fact);
    }
  }

  private void mockFetchBindings() {
    for (int i = 0; i < objects.size(); i++) {
      ObjectFactBindingEntity binding = new ObjectFactBindingEntity()
              .setObjectID(objects.get(i).getId())
              .setFactID(facts.get(i).getId());
      when(getObjectManager().fetchObjectFactBindings(objects.get(i).getId())).thenReturn(ListUtils.list(binding));
    }
  }

}
