package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FactSearchMetaDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testSearchMetaFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    FactSearchMetaDelegate.create().handle(new SearchMetaFactsRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testSearchMetaFactsNonExistingFact() throws Exception {
    FactSearchMetaDelegate.create().handle(new SearchMetaFactsRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchMetaFactsNoAccessToFact() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID());
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactEntity.class));

    FactSearchMetaDelegate.create().handle(request);
  }

  @Test
  public void testSearchMetaFactsPopulateCriteria() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest()
            .setFact(UUID.randomUUID())
            .addFactValue("factValue");
    mockSearchMetaFacts();

    FactSearchMetaDelegate.create().handle(request);

    verify(getFactSearchManager()).searchFacts(argThat(criteria -> {
      assertTrue(criteria.getInReferenceTo().size() > 0);
      assertTrue(criteria.getFactValue().size() > 0);
      assertTrue(criteria.getAvailableOrganizationID().size() > 0);
      assertNotNull(criteria.getCurrentUserID());
      return true;
    }));
  }

  @Test
  public void testSearchMetaFactsFilterNonAccessibleFacts() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID());

    mockSearchMetaFacts();
    when(getSecurityContext().hasReadPermission(isA(FactEntity.class))).thenReturn(false);

    ResultSet<Fact> result = FactSearchMetaDelegate.create().handle(request);
    assertEquals(100, result.getCount());
    assertEquals(0, result.getValues().size());

    verify(getFactSearchManager()).searchFacts(any());
    verify(getFactManager()).getFacts(argThat(i -> !i.isEmpty()));
  }

  @Test
  public void testSearchMetaFactsNoResult() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID());

    mockSearchMetaFacts();
    when(getFactSearchManager().searchFacts(any())).thenReturn(SearchResult.<FactDocument>builder().build());
    when(getFactManager().getFacts(any())).thenReturn(Collections.emptyIterator());

    ResultSet<Fact> result = FactSearchMetaDelegate.create().handle(request);
    assertEquals(0, result.getCount());
    assertEquals(0, result.getValues().size());

    verify(getFactSearchManager()).searchFacts(any());
    verify(getFactManager()).getFacts(argThat(List::isEmpty));
  }

  @Test
  public void testSearchMetaFacts() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID());

    mockSearchMetaFacts();

    ResultSet<Fact> result = FactSearchMetaDelegate.create().handle(request);
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, result.getValues().size());

    verify(getFactSearchManager()).searchFacts(any());
    verify(getFactManager()).getFacts(any());
    verify(getFactConverter()).apply(any());
    verify(getSecurityContext()).hasReadPermission(isA(FactEntity.class));
    verify(getSecurityContext()).checkReadPermission(isA(FactEntity.class));
  }

  private void mockSearchMetaFacts() {
    UUID factID = UUID.randomUUID();
    SearchResult<FactDocument> result = SearchResult.<FactDocument>builder()
            .setLimit(25)
            .setCount(100)
            .addValue(new FactDocument().setId(factID))
            .build();

    when(getFactManager().getFact(any())).thenReturn(new FactEntity());

    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(getSecurityContext().hasReadPermission(isA(FactEntity.class))).thenReturn(true);
    when(getFactSearchManager().searchFacts(any())).thenReturn(result);
    when(getFactManager().getFacts(any())).thenReturn(Collections.singleton(new FactEntity().setId(factID)).iterator());
    when(getFactConverter().apply(any())).thenReturn(Fact.builder().setId(factID).build());
  }

}
