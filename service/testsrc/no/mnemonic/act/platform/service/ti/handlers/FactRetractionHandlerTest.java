package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.result.ScrollingSearchResult;
import no.mnemonic.act.platform.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactRetractionHandlerTest {

  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private AccessControlCriteriaResolver accessControlCriteriaResolver;

  private FactRetractionHandler handler;

  @Before
  public void setUp() {
    initMocks(this);

    // Common mocks used by most tests.
    when(factTypeRequestResolver.resolveRetractionFactType()).thenReturn(new FactTypeEntity().setId(UUID.randomUUID()));
    when(accessControlCriteriaResolver.get()).thenReturn(AccessControlCriteria.builder()
            .addCurrentUserIdentity(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build());

    handler = new FactRetractionHandler(factTypeRequestResolver, factSearchManager, accessControlCriteriaResolver);
  }

  @Test
  public void testIsRetractedWithNullInput() {
    assertFalse(handler.isRetracted(null));
  }

  @Test
  public void testIsRetractedWithoutRetractedHintNotRetracted() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());

    assertFalse(handler.isRetracted(fact));

    verifyNoMoreInteractions(factSearchManager);
  }

  @Test
  public void testIsRetractedPopulatesSearchCriteria() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));

    assertFalse(handler.isRetracted(fact));

    verify(factSearchManager).searchFacts(argThat(criteria -> {
      assertEquals(set(fact.getId()), criteria.getInReferenceTo());
      assertFalse(criteria.getFactTypeID().isEmpty());
      assertNotNull(criteria.getAccessControlCriteria());
      return true;
    }));
  }

  @Test
  public void testIsRetractedWithRetractedHintTrue() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactDocument retraction = new FactDocument().setId(UUID.randomUUID());
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction));

    assertTrue(handler.isRetracted(fact));

    verify(factSearchManager).searchFacts(inReferenceTo(fact.getId()));
    verify(factSearchManager).searchFacts(inReferenceTo(retraction.getId()));
    verifyNoMoreInteractions(factSearchManager);
  }

  @Test
  public void testIsRetractedWithRetractedRetraction() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());

    // fact ---> retraction1 ---> retraction2
    // retraction2 cancels out retraction1, thus, fact in not retracted.
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction1));
    when(factSearchManager.searchFacts(inReferenceTo(retraction1.getId()))).thenReturn(createSearchResult(retraction2));

    assertFalse(handler.isRetracted(fact));
  }

  @Test
  public void testIsRetractedWithRetractedRetractionTwoLevels() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactDocument retraction1 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction2 = new FactDocument().setId(UUID.randomUUID());
    FactDocument retraction3 = new FactDocument().setId(UUID.randomUUID());

    // fact ---> retraction1 ---> retraction2 ---> retraction3
    // retraction3 cancels out retraction2, thus, fact is retracted because retraction1 holds.
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction1));
    when(factSearchManager.searchFacts(inReferenceTo(retraction1.getId()))).thenReturn(createSearchResult(retraction2));
    when(factSearchManager.searchFacts(inReferenceTo(retraction2.getId()))).thenReturn(createSearchResult(retraction3));

    assertTrue(handler.isRetracted(fact));
  }

  @Test
  public void testIsRetractedWithRetractedRetractionComplexTree() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
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

    assertTrue(handler.isRetracted(fact));
  }

  @Test
  public void testIsRetractedCachesResult() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactDocument retraction = new FactDocument().setId(UUID.randomUUID());
    when(factSearchManager.searchFacts(inReferenceTo(fact.getId()))).thenReturn(createSearchResult(retraction));

    assertTrue(handler.isRetracted(fact));

    verify(factSearchManager).searchFacts(inReferenceTo(fact.getId()));
    verify(factSearchManager).searchFacts(inReferenceTo(retraction.getId()));
    verifyNoMoreInteractions(factSearchManager);

    assertTrue(handler.isRetracted(fact));
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
