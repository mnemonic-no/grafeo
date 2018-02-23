package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ObjectSearchFactsDelegateTest extends AbstractDelegateTest {

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
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

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
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

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
  public void testSearchObjectFactsByIdPopulateCriteria() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectID(UUID.randomUUID())
            .addFactValue("factValue");
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValuePopulateCriteria() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectType("type")
            .setObjectValue("value")
            .addFactValue("factValue");
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsByIdFilterNonAccessibleFacts() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(UUID.randomUUID());
    testFilterNonAccessibleFacts(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValueFilterNonAccessibleFacts() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");
    testFilterNonAccessibleFacts(request);
  }

  @Test
  public void testSearchObjectFactsById() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(UUID.randomUUID());
    testSearchObjectFacts(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValue() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");
    testSearchObjectFacts(request);
  }

  private void testPopulateCriteria(SearchObjectFactsRequest request) throws Exception {
    mockSearchObjectFacts();
    ObjectSearchFactsDelegate.create().handle(request);
    verify(getFactSearchManager()).searchFacts(argThat(criteria -> {
      assertTrue(criteria.getObjectTypeName().size() == 0);
      assertTrue(criteria.getObjectValue().size() == 0);
      assertTrue(criteria.getObjectID().size() > 0);
      assertTrue(criteria.getFactValue().size() > 0);
      assertTrue(criteria.getAvailableOrganizationID().size() > 0);
      assertNotNull(criteria.getCurrentUserID());
      return true;
    }));
  }

  private void testFilterNonAccessibleFacts(SearchObjectFactsRequest request) throws Exception {
    mockSearchObjectFacts();
    when(getSecurityContext().hasReadPermission(isA(FactEntity.class))).thenReturn(false);

    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(0, result.getValues().size());
  }

  private void testSearchObjectFacts(SearchObjectFactsRequest request) throws Exception {
    mockSearchObjectFacts();

    ResultSet<Fact> result = ObjectSearchFactsDelegate.create().handle(request);
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, result.getValues().size());

    verify(getFactSearchManager()).searchFacts(any());
    verify(getFactManager()).getFacts(any());
    verify(getFactConverter()).apply(any());
    verify(getSecurityContext()).hasReadPermission(isA(FactEntity.class));
    verify(getSecurityContext()).checkReadPermission(isA(ObjectEntity.class));
  }

  private void mockSearchObjectFacts() {
    UUID factID = UUID.randomUUID();
    SearchResult<FactDocument> result = SearchResult.<FactDocument>builder()
            .setLimit(25)
            .setCount(100)
            .addValue(new FactDocument().setId(factID))
            .build();

    when(getObjectManager().getObjectType(isA(String.class))).thenReturn(new ObjectTypeEntity());
    when(getObjectManager().getObject(any())).thenReturn(new ObjectEntity().setId(UUID.randomUUID()));
    when(getObjectManager().getObject(any(), any())).thenReturn(new ObjectEntity().setId(UUID.randomUUID()));

    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(getSecurityContext().hasReadPermission(isA(FactEntity.class))).thenReturn(true);
    when(getFactSearchManager().searchFacts(any())).thenReturn(result);
    when(getFactManager().getFacts(any())).thenReturn(Collections.singleton(new FactEntity().setId(factID)).iterator());
    when(getFactConverter().apply(any())).thenReturn(Fact.builder().setId(factID).build());
  }

}
