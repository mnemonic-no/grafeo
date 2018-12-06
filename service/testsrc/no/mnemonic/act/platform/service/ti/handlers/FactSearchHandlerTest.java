package no.mnemonic.act.platform.service.ti.handlers;

import com.google.common.collect.Iterators;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ScrollingSearchResult;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactSearchHandlerTest {

  @Mock
  private FactTypeResolver factTypeResolver;
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
    when(factTypeResolver.resolveRetractionFactType()).thenReturn(new FactTypeEntity().setId(UUID.randomUUID()));
    when(securityContext.hasReadPermission(isA(FactEntity.class))).thenReturn(true);
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(factConverter.apply(any())).thenReturn(factModel);

    handler = FactSearchHandler.builder()
            .setFactTypeResolver(factTypeResolver)
            .setFactSearchManager(factSearchManager)
            .setFactManager(factManager)
            .setSecurityContext(securityContext)
            .setFactConverter(factConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateHandlerWithoutFactTypeResolver() {
    FactSearchHandler.builder()
            .setFactSearchManager(factSearchManager)
            .setFactManager(factManager)
            .setSecurityContext(securityContext)
            .setFactConverter(factConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateHandlerWithoutFactSearchManager() {
    FactSearchHandler.builder()
            .setFactTypeResolver(factTypeResolver)
            .setFactManager(factManager)
            .setSecurityContext(securityContext)
            .setFactConverter(factConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateHandlerWithoutFactManager() {
    FactSearchHandler.builder()
            .setFactTypeResolver(factTypeResolver)
            .setFactSearchManager(factSearchManager)
            .setSecurityContext(securityContext)
            .setFactConverter(factConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateHandlerWithoutSecurityContext() {
    FactSearchHandler.builder()
            .setFactTypeResolver(factTypeResolver)
            .setFactSearchManager(factSearchManager)
            .setFactManager(factManager)
            .setFactConverter(factConverter)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateHandlerWithoutFactConverter() {
    FactSearchHandler.builder()
            .setFactTypeResolver(factTypeResolver)
            .setFactSearchManager(factSearchManager)
            .setFactManager(factManager)
            .setSecurityContext(securityContext)
            .build();
  }

  @Test
  public void testSearchFactsWithLimit() {
    mockSimpleSearch();

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, result.getValues().size());
  }

  @Test
  public void testSearchFactsWithoutLimit() {
    mockSimpleSearch();

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(0));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(10000, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, result.getValues().size());
  }

  @Test
  public void testSearchFactsWithLimitAboveMaxLimit() {
    mockSimpleSearch();

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(10001));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(10000, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, result.getValues().size());
  }

  @Test
  public void testSearchFactsNoResults() {
    when(factSearchManager.searchFacts(any())).thenReturn(createSearchResult());
    when(factManager.getFacts(any())).thenReturn(Collections.emptyIterator());

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(0, result.getCount());
    assertEquals(0, result.getValues().size());

    verify(factSearchManager).searchFacts(criteria);
    verify(factManager).getFacts(argThat(List::isEmpty));
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
    assertEquals(2, result.getValues().size());

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
    assertEquals(25, result.getValues().size());

    verify(factSearchManager).searchFacts(criteria);
    verify(factManager, times(5)).getFacts(argThat(i -> i.contains(factID)));
    verify(securityContext, times(25)).hasReadPermission(factEntity);
    verify(factConverter, times(25)).apply(factEntity);
  }

  @Test
  public void testSearchFactsIncludeRetracted() {
    factDocument.setRetracted(true);

    FactSearchCriteria criteria = mockSearchWithRetraction();
    handler.search(criteria, true);

    verify(factManager).getFacts(argThat(i -> i.contains(factDocument.getId())));
    verify(factSearchManager).searchFacts(criteria);
    verifyNoMoreInteractions(factSearchManager);
  }

  @Test
  public void testSearchFactsExcludeRetractedNotRetracted() {
    factDocument.setRetracted(false);

    FactSearchCriteria criteria = mockSearchWithRetraction();
    handler.search(criteria, false);

    verify(factManager).getFacts(argThat(i -> i.contains(factDocument.getId())));
    verify(factSearchManager).searchFacts(criteria);
    verifyNoMoreInteractions(factSearchManager);
  }

  @Test
  public void testSearchFactsExcludeRetracted() {
    factDocument.setRetracted(true);

    FactSearchCriteria criteria = mockSearchWithRetraction();
    handler.search(criteria, false);

    verify(factManager).getFacts(argThat(i -> !i.contains(factDocument.getId())));
    verify(factSearchManager, times(3)).searchFacts(argThat(i -> {
      // Called once for the actual Fact search.
      if (i == criteria) return true;

      // Called twice for checking retractions.
      assertFalse(i.getInReferenceTo().isEmpty());
      assertFalse(i.getFactTypeID().isEmpty());
      assertFalse(i.getAvailableOrganizationID().isEmpty());
      assertNotNull(i.getCurrentUserID());
      return true;
    }));
  }

  @Test
  public void testSearchFactsExcludeRetractedWithRetractedRetraction() {
    factDocument.setRetracted(true);
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);

    // factDocument ---> retraction1 ---> retraction2
    // retraction2 cancels out retraction1, thus, factDocument in not retracted.
    when(factSearchManager.searchFacts(criteria))
            .thenReturn(createSearchResult(factDocument));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(factDocument.getId()))))
            .thenReturn(createSearchResult(retraction1));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(retraction1.getId()))))
            .thenReturn(createSearchResult(retraction2));
    when(factManager.getFacts(any())).thenReturn(Collections.emptyIterator());

    handler.search(criteria, false);

    verify(factManager).getFacts(argThat(i -> i.contains(factDocument.getId())));
  }

  @Test
  public void testSearchFactsExcludeRetractedWithRetractedRetractionTwoLevels() {
    factDocument.setRetracted(true);
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction3 = new FactDocument().setId(UUID.randomUUID());

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);

    // factDocument ---> retraction1 ---> retraction2 ---> retraction3
    // retraction3 cancels out retraction2, thus, factDocument is retracted because retraction1 holds.
    when(factSearchManager.searchFacts(criteria))
            .thenReturn(createSearchResult(factDocument));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(factDocument.getId()))))
            .thenReturn(createSearchResult(retraction1));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(retraction1.getId()))))
            .thenReturn(createSearchResult(retraction2));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(retraction2.getId()))))
            .thenReturn(createSearchResult(retraction3));
    when(factManager.getFacts(any())).thenReturn(Collections.emptyIterator());

    handler.search(criteria, false);

    verify(factManager).getFacts(argThat(i -> !i.contains(factDocument.getId())));
  }

  @Test
  public void testSearchFactsExcludeRetractedWithRetractedRetractionComplexTree() {
    factDocument.setRetracted(true);
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction3 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction4 = new FactDocument().setId(UUID.randomUUID());

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);

    // factDocument ---> retraction1 ---> retraction2
    //     |-----------> retraction3
    //     |-----------> retraction4
    // retraction2 cancels out retraction1, thus, factDocument is retracted because of retraction3/retraction4.
    when(factSearchManager.searchFacts(criteria))
            .thenReturn(createSearchResult(factDocument));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(factDocument.getId()))))
            .thenReturn(createSearchResult(retraction1, retraction3, retraction4));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(retraction1.getId()))))
            .thenReturn(createSearchResult(retraction2));
    when(factManager.getFacts(any())).thenReturn(Collections.emptyIterator());

    handler.search(criteria, false);

    verify(factManager).getFacts(argThat(i -> !i.contains(factDocument.getId())));
  }

  private void mockSimpleSearch() {
    when(factSearchManager.searchFacts(any())).thenReturn(ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID",
                    Collections.singleton(factDocument).iterator(), true))
            .setCount(100)
            .build());
    when(factManager.getFacts(any())).thenReturn(Collections.singleton(factEntity).iterator());
  }

  private FactSearchCriteria mockSearchWithRetraction() {
    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    FactDocument retraction = new FactDocument().setId(UUID.randomUUID());

    when(factSearchManager.searchFacts(criteria))
            .thenReturn(createSearchResult(factDocument));
    when(factSearchManager.searchFacts(argThat(i -> i != null && i.getInReferenceTo() != null && i.getInReferenceTo().contains(factDocument.getId()))))
            .thenReturn(createSearchResult(retraction));
    when(factManager.getFacts(any())).thenReturn(Collections.emptyIterator());

    return criteria;
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
