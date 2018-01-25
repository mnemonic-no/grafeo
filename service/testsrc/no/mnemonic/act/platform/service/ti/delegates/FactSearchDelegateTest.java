package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class FactSearchDelegateTest extends AbstractDelegateTest {

  @Before
  public void setup() {
    UUID factID = UUID.randomUUID();
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(getSecurityContext().hasReadPermission(any())).thenReturn(true);
    when(getFactSearchManager().searchFacts(any())).thenReturn(createSearchResult(factID));
    when(getFactManager().getFacts(any())).thenReturn(Collections.singleton(new FactEntity().setId(factID)).iterator());
    when(getFactConverter().apply(any())).thenReturn(Fact.builder().setId(factID).build());
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    FactSearchDelegate.create().handle(new SearchFactRequest());
  }

  @Test
  public void testSearchFactsPopulateCriteria() throws Exception {
    FactSearchDelegate.create().handle(new SearchFactRequest().addFactValue("value"));
    verify(getFactSearchManager()).searchFacts(argThat(criteria -> {
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
      assertNotNull(criteria.getFactValue());
      return true;
    }));
  }

  @Test
  public void testSearchFactsFilterNonAccessibleFacts() throws Exception {
    when(getSecurityContext().hasReadPermission(any())).thenReturn(false);
    ResultSet<Fact> result = FactSearchDelegate.create().handle(new SearchFactRequest());
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(0, result.getValues().size());
  }

  @Test
  public void testSearchFacts() throws Exception {
    ResultSet<Fact> result = FactSearchDelegate.create().handle(new SearchFactRequest());
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, result.getValues().size());

    verify(getFactSearchManager()).searchFacts(any());
    verify(getFactManager()).getFacts(any());
    verify(getFactConverter()).apply(any());
    verify(getSecurityContext()).hasReadPermission(any());
  }

  private SearchResult<FactDocument> createSearchResult(UUID factID) {
    return SearchResult.builder()
            .setLimit(25)
            .setCount(100)
            .addValue(new FactDocument().setId(factID))
            .build();
  }

}
