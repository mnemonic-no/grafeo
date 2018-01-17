package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver.RETRACTION_FACT_TYPE_ID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ObjectSearchFactsDelegateTest extends AbstractDelegateTest {

  private ObjectTypeEntity objectType = new ObjectTypeEntity()
          .setId(UUID.randomUUID())
          .setName("objectType");
  private ObjectEntity object = new ObjectEntity()
          .setId(UUID.randomUUID())
          .setTypeID(objectType.getId())
          .setValue("objectValue");
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
  private FactEntity retractionFact = new FactEntity()
          .setId(UUID.randomUUID())
          .setTypeID(RETRACTION_FACT_TYPE_ID)
          .setInReferenceToID(fact2.getId());
  private List<FactEntity> facts = ListUtils.list(fact1, fact2, fact3);

  @Test(expected = AccessDeniedException.class)
  public void testSearchObjectFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectSearchFactsDelegate.create().handle(new SearchObjectFactsRequest());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchObjectFactsWithoutSpecifiedObject() throws Exception {
    ObjectSearchFactsDelegate.create().handle(new SearchObjectFactsRequest());
  }

  @Test
  public void testSearchObjectFactsWithNonExistingObjectType() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");

    try {
      ObjectSearchFactsDelegate.create().handle(request);
      fail();
    } catch (InvalidArgumentException ignored) {
      verify(getObjectManager()).getObjectType(request.getObjectType());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testSearchObjectFactsByNonExistingId() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(UUID.randomUUID());

    try {
      ObjectSearchFactsDelegate.create().handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObject(request.getObjectID());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testSearchObjectFactsByNonExistingTypeValue() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");
    when(getObjectManager().getObjectType(request.getObjectType())).thenReturn(new ObjectTypeEntity());

    try {
      ObjectSearchFactsDelegate.create().handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObjectType(request.getObjectType());
      verify(getObjectManager()).getObject(request.getObjectType(), request.getObjectValue());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testSearchObjectFactsWithoutFacts() throws Exception {
    mockFetchObject();
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId());

    try {
      ObjectSearchFactsDelegate.create().handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).fetchObjectFactBindings(object.getId());
      verify(getFactManager(), never()).getFact(any());
    }
  }

  @Test
  public void testSearchObjectFactsWithoutAccessToFacts() throws Exception {
    mockFetchObject();
    mockFetchFacts();
    mockFetchBindings();
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId());

    try {
      ObjectSearchFactsDelegate.create().handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).fetchObjectFactBindings(object.getId());
      verify(getFactManager(), times(facts.size())).getFact(any());
      verify(getSecurityContext(), times(facts.size())).hasReadPermission(any());
    }
  }

  @Test
  public void testSearchObjectFactsFilterByFactType() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).addFactType(factType2.getName());
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(fact3.getId(), result.getValues().iterator().next().getId());
  }

  @Test
  public void testSearchObjectFactsFilterByFactValue() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).addFactValue(fact1.getValue());
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(fact1.getId(), result.getValues().iterator().next().getId());
  }

  @Test
  public void testSearchObjectFactsFilterBySource() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).addSource(fact2.getSourceID().toString());
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(fact2.getId(), result.getValues().iterator().next().getId());
  }

  @Test
  public void testSearchObjectFactsIncludeRetracted() throws Exception {
    ListUtils.addToList(facts, retractionFact);
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).setIncludeRetracted(true);
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(4, result.getCount());
    assertEquals(4, result.getValues().size());
    assertTrue(result.getValues().stream().anyMatch(f -> Objects.equals(f.getId(), fact2.getId())));
  }

  @Test
  public void testSearchObjectFactsExcludeRetracted() throws Exception {
    ListUtils.addToList(facts, retractionFact);
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).setIncludeRetracted(false);
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(3, result.getCount());
    assertEquals(3, result.getValues().size());
    assertFalse(result.getValues().stream().anyMatch(f -> Objects.equals(f.getId(), fact2.getId())));
  }

  @Test
  public void testSearchObjectFactsExcludeRetractedByDefault() throws Exception {
    ListUtils.addToList(facts, retractionFact);
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId());
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(3, result.getCount());
    assertEquals(3, result.getValues().size());
    assertFalse(result.getValues().stream().anyMatch(f -> Objects.equals(f.getId(), fact2.getId())));
  }

  @Test
  public void testSearchObjectFactsFilterTimestampBefore() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).setBefore(2000L);
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(fact1.getId(), result.getValues().iterator().next().getId());
  }

  @Test
  public void testSearchObjectFactsFilterTimestampAfter() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).setAfter(3000L);
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(fact3.getId(), result.getValues().iterator().next().getId());
  }

  @Test
  public void testSearchObjectFactsCombineMultipleFilters() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId())
            .addFactType(factType1.getName())
            .addFactValue(fact1.getValue())
            .addSource(fact1.getSourceID().toString())
            .setIncludeRetracted(true)
            .setBefore(2000L)
            .setAfter(1000L);
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(fact1.getId(), result.getValues().iterator().next().getId());
  }

  @Test
  public void testSearchObjectFactsLimitedResult() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).setLimit(1);
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals((long) request.getLimit(), result.getLimit());
    assertEquals(facts.size(), result.getCount());
    assertEquals((long) request.getLimit(), result.getValues().size());
  }

  @Test
  public void testSearchObjectFactsUnlimitedResult() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId()).setLimit(0);
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals((long) request.getLimit(), result.getLimit());
    assertEquals(facts.size(), result.getCount());
    assertEquals(facts.size(), result.getValues().size());
  }

  @Test
  public void testSearchObjectFactsWithDefaultLimit() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId());
    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);

    assertEquals(25, result.getLimit());
    assertEquals(facts.size(), result.getCount());
    assertEquals(facts.size(), result.getValues().size());
  }

  @Test
  public void testSearchObjectFactsById() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(object.getId());
    ObjectSearchFactsDelegate.create().handle(request);

    verify(getObjectManager()).getObject(object.getId());
    verify(getObjectManager()).fetchObjectFactBindings(object.getId());
    verify(getFactManager(), times(facts.size())).getFact(any());
    verify(getFactConverter(), times(facts.size())).apply(any());
  }

  @Test
  public void testSearchObjectFactsByTypeValue() throws Exception {
    mockEverything();

    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType(objectType.getName()).setObjectValue(object.getValue());
    ObjectSearchFactsDelegate.create().handle(request);

    verify(getObjectManager()).getObjectType(objectType.getName());
    verify(getObjectManager()).getObject(objectType.getName(), object.getValue());
    verify(getObjectManager()).fetchObjectFactBindings(object.getId());
    verify(getFactManager(), times(facts.size())).getFact(any());
    verify(getFactConverter(), times(facts.size())).apply(any());
  }

  private void mockEverything() {
    when(getSecurityContext().hasReadPermission(any())).thenReturn(true);
    mockFetchObject();
    mockFetchFacts();
    mockFetchBindings();
  }

  private void mockFetchObject() {
    when(getObjectManager().getObjectType(objectType.getId())).thenReturn(objectType);
    when(getObjectManager().getObjectType(objectType.getName())).thenReturn(objectType);
    when(getObjectManager().getObject(object.getId())).thenReturn(object);
    when(getObjectManager().getObject(objectType.getName(), object.getValue())).thenReturn(object);
  }

  private void mockFetchFacts() {
    when(getFactManager().getFactType(factType1.getId())).thenReturn(factType1);
    when(getFactManager().getFactType(factType1.getName())).thenReturn(factType1);
    when(getFactManager().getFactType(factType2.getId())).thenReturn(factType2);
    when(getFactManager().getFactType(factType2.getName())).thenReturn(factType2);

    for (FactEntity fact : facts) {
      when(getFactManager().getFact(fact.getId())).thenReturn(fact);
    }

    when(getFactConverter().apply(any())).thenAnswer(i -> {
      FactEntity entity = i.getArgument(0);
      return Fact.builder().setId(entity.getId()).build();
    });
  }

  private void mockFetchBindings() {
    List<ObjectFactBindingEntity> bindings = facts.stream()
            .map(fact -> new ObjectFactBindingEntity().setObjectID(object.getId()).setFactID(fact.getId()))
            .collect(Collectors.toList());

    when(getObjectManager().fetchObjectFactBindings(object.getId())).thenReturn(bindings);
  }

}
