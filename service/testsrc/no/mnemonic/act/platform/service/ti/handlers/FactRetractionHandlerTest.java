package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ScrollingSearchResult;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactRetractionHandlerTest {

  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private TiSecurityContext securityContext;

  private FactRetractionHandler handler;

  @Before
  public void setUp() {
    initMocks(this);

    // Common mocks used by most tests.
    when(factTypeResolver.resolveRetractionFactType()).thenReturn(new FactTypeEntity().setId(UUID.randomUUID()));
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));

    handler = new FactRetractionHandler(factTypeResolver, factSearchManager, securityContext);
  }

  @Test
  public void testIsRetractedWithNullInput() {
    assertFalse(handler.isRetracted(null));
    assertFalse(handler.isRetracted(null, null));
  }

  @Test
  public void testIsRetractedWithoutRetractedHintNotRetracted() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());

    assertFalse(handler.isRetracted(fact.getId()));

    verify(factSearchManager).searchFacts(inReferenceTo(fact.getId()));
    verifyNoMoreInteractions(factSearchManager);
  }

  @Test
  public void testIsRetractedWithoutRetractedHintRetracted() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction = new FactDocument().setId(UUID.randomUUID());
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction));

    assertTrue(handler.isRetracted(fact.getId()));

    verify(factSearchManager).searchFacts(inReferenceTo(fact.getId()));
    verify(factSearchManager).searchFacts(inReferenceTo(retraction.getId()));
    verifyNoMoreInteractions(factSearchManager);
  }

  @Test
  public void testIsRetractedWithRetractedHintFalse() {
    assertFalse(handler.isRetracted(UUID.randomUUID(), false));
    verifyZeroInteractions(factSearchManager);
  }

  @Test
  public void testIsRetractedWithRetractedHintTrue() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction = new FactDocument().setId(UUID.randomUUID());
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction));

    assertTrue(handler.isRetracted(fact.getId(), true));

    verify(factSearchManager).searchFacts(inReferenceTo(fact.getId()));
    verify(factSearchManager).searchFacts(inReferenceTo(retraction.getId()));
    verifyNoMoreInteractions(factSearchManager);
  }

  @Test
  public void testIsRetractedPopulatesSearchCriteria() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());

    assertFalse(handler.isRetracted(fact.getId()));

    verify(factSearchManager).searchFacts(argThat(criteria -> {
      assertEquals(set(fact.getId()), criteria.getInReferenceTo());
      assertFalse(criteria.getFactTypeID().isEmpty());
      assertFalse(criteria.getAvailableOrganizationID().isEmpty());
      assertNotNull(criteria.getCurrentUserID());
      return true;
    }));
  }

  @Test
  public void testIsRetractedWithRetractedRetraction() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());

    // fact ---> retraction1 ---> retraction2
    // retraction2 cancels out retraction1, thus, fact in not retracted.
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction1));
    when(factSearchManager.searchFacts(inReferenceTo(retraction1.getId()))).thenReturn(createSearchResult(retraction2));

    assertFalse(handler.isRetracted(fact.getId()));
  }

  @Test
  public void testIsRetractedWithRetractedRetractionTwoLevels() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction3 = new FactDocument().setId(UUID.randomUUID());

    // fact ---> retraction1 ---> retraction2 ---> retraction3
    // retraction3 cancels out retraction2, thus, fact is retracted because retraction1 holds.
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction1));
    when(factSearchManager.searchFacts(inReferenceTo(retraction1.getId()))).thenReturn(createSearchResult(retraction2));
    when(factSearchManager.searchFacts(inReferenceTo(retraction2.getId()))).thenReturn(createSearchResult(retraction3));

    assertTrue(handler.isRetracted(fact.getId()));
  }

  @Test
  public void testIsRetractedWithRetractedRetractionComplexTree() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction3 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction4 = new FactDocument().setId(UUID.randomUUID());

    // fact -----------> retraction1 ---> retraction2
    //     |-----------> retraction3
    //     |-----------> retraction4
    // retraction2 cancels out retraction1, thus, factDocument is retracted because of retraction3/retraction4.
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction1, retraction3, retraction4));
    when(factSearchManager.searchFacts(inReferenceTo(retraction1.getId()))).thenReturn(createSearchResult(retraction2));

    assertTrue(handler.isRetracted(fact.getId()));
  }

  @Test
  public void testIsRetractedCachesResult() {
    FactDocument fact = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction = new FactDocument().setId(UUID.randomUUID());
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction));

    assertTrue(handler.isRetracted(fact.getId()));

    verify(factSearchManager).searchFacts(inReferenceTo(fact.getId()));
    verify(factSearchManager).searchFacts(inReferenceTo(retraction.getId()));
    verifyNoMoreInteractions(factSearchManager);

    assertTrue(handler.isRetracted(fact.getId()));
    verifyNoMoreInteractions(factSearchManager);
  }

  private ScrollingSearchResult<FactDocument> createSearchResult(FactDocument... fact) {
    return ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", list(fact).iterator(), true))
            .setCount(list(fact).size())
            .build();
  }

  private FactSearchCriteria inReferenceTo(UUID id) {
    return argThat(criteria -> criteria != null && criteria.getInReferenceTo() != null && criteria.getInReferenceTo().contains(id));
  }
}
