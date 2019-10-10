package no.mnemonic.act.platform.service.ti.handlers;

import com.google.common.collect.Iterators;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.result.ScrollingSearchResult;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactSearchHandlerTest {

  @Mock
  private FactRetractionHandler retractionHandler;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private FactManager factManager;
  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private Function<FactEntity, Fact> factConverter;

  private final UUID factID = UUID.randomUUID();
  private final FactDocument factDocument = new FactDocument().setId(factID);
  private final FactEntity factEntity = new FactEntity().setId(factID);
  private final Fact factModel = Fact.builder().setId(factID).build();

  private FactSearchHandler handler;

  @Before
  public void setUp() {
    initMocks(this);

    // Common mocks used by most tests.
    when(securityContext.hasReadPermission(isA(FactEntity.class))).thenReturn(true);
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(factConverter.apply(any())).thenReturn(factModel);

    handler = new FactSearchHandler(retractionHandler, factSearchManager, factManager, securityContext, factConverter);
  }

  @Test
  public void testSearchFactsWithLimit() {
    mockSimpleSearch();

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());
  }

  @Test
  public void testSearchFactsWithoutLimit() {
    mockSimpleSearch();

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(0));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(10000, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());
  }

  @Test
  public void testSearchFactsWithLimitAboveMaxLimit() {
    mockSimpleSearch();

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(10001));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(10000, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());
  }

  @Test
  public void testSearchFactsNoResults() {
    when(factSearchManager.searchFacts(any())).thenReturn(createSearchResult());
    when(factManager.getFacts(any())).thenReturn(Collections.emptyIterator());

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(0, result.getCount());
    assertEquals(0, ListUtils.list(result.iterator()).size());

    verify(factSearchManager).searchFacts(criteria);
    verify(factManager, atLeastOnce()).getFacts(argThat(List::isEmpty));
  }

  @Test
  public void testSearchFactsFiltersNonAccessibleFacts() {
    when(factSearchManager.searchFacts(any())).thenReturn(createSearchResult(factDocument, factDocument, factDocument));
    when(factManager.getFacts(any())).thenReturn(ListUtils.list(factEntity, factEntity, factEntity).iterator());
    when(securityContext.hasReadPermission(isA(FactEntity.class))).thenReturn(true, false, true);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(3, result.getCount());
    assertEquals(2, ListUtils.list(result.iterator()).size());

    verify(factSearchManager).searchFacts(criteria);
    verify(factManager).getFacts(argThat(i -> i.size() == 3));
    verify(securityContext, times(3)).hasReadPermission(factEntity);
    verify(factConverter, times(2)).apply(factEntity);
  }

  @Test
  public void testSearchFactsFetchesUntilLimit() {
    when(factSearchManager.searchFacts(any())).thenReturn(ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID",
                    Iterators.cycle(factDocument), true))
            .setCount(100)
            .build());
    when(factManager.getFacts(any())).then(i -> Iterators.limit(Iterators.cycle(factEntity), 5));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(25, ListUtils.list(result.iterator()).size());

    verify(factSearchManager).searchFacts(criteria);
    verify(factManager, times(5)).getFacts(argThat(i -> i.contains(factID)));
    verify(securityContext, times(25)).hasReadPermission(factEntity);
    verify(factConverter, times(25)).apply(factEntity);
  }

  @Test
  public void testSearchFactsIncludeRetracted() {
    factDocument.setRetracted(true);

    mockSimpleSearch();
    when(retractionHandler.isRetracted(factID, true)).thenReturn(true);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    ListUtils.list(handler.search(criteria, true).iterator());

    verify(factManager).getFacts(argThat(i -> i.contains(factDocument.getId())));
    verify(retractionHandler).isRetracted(factID, true);
  }

  @Test
  public void testSearchFactsExcludeRetractedNotRetracted() {
    factDocument.setRetracted(false);

    mockSimpleSearch();
    when(retractionHandler.isRetracted(factID, false)).thenReturn(false);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    ListUtils.list(handler.search(criteria, false).iterator());

    verify(factManager).getFacts(argThat(i -> i.contains(factDocument.getId())));
    verify(retractionHandler).isRetracted(factID, false);
  }

  @Test
  public void testSearchFactsExcludeRetracted() {
    factDocument.setRetracted(true);

    mockSimpleSearch();
    when(retractionHandler.isRetracted(factID, true)).thenReturn(true);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    ListUtils.list(handler.search(criteria, false).iterator());

    verify(factManager, atLeastOnce()).getFacts(argThat(i -> !i.contains(factDocument.getId())));
    verify(retractionHandler).isRetracted(factID, true);
  }

  private void mockSimpleSearch() {
    when(factSearchManager.searchFacts(any())).thenReturn(ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID",
                    Collections.singleton(factDocument).iterator(), true))
            .setCount(100)
            .build());
    when(factManager.getFacts(any())).thenReturn(Collections.singleton(factEntity).iterator());
  }

  private ScrollingSearchResult<FactDocument> createSearchResult(FactDocument... fact) {
    return ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", ListUtils.list(fact).iterator(), true))
            .setCount(ListUtils.list(fact).size())
            .build();
  }

  private FactSearchCriteria createFactSearchCriteria(ObjectPreparation<FactSearchCriteria.Builder> preparation) {
    FactSearchCriteria.Builder builder = FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID());
    if (preparation != null) {
      builder = preparation.prepare(builder);
    }
    return builder.build();
  }

  private interface ObjectPreparation<T> {
    T prepare(T e);
  }
}
