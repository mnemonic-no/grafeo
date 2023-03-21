package no.mnemonic.services.grafeo.service.ti.handlers;

import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.resolvers.request.FactTypeRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactRetractionHandlerTest {

  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private ObjectFactDao objectFactDao;

  private FactRetractionHandler handler;

  private final UUID retractionFactTypeID = UUID.randomUUID();

  @Before
  public void setUp() {
    initMocks(this);

    // Common mocks used by most tests.
    when(factTypeRequestResolver.resolveRetractionFactType()).thenReturn(new FactTypeEntity().setId(retractionFactTypeID));
    when(securityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(true);
    when(objectFactDao.retrieveMetaFacts(any())).thenReturn(Collections.emptyIterator());

    handler = new FactRetractionHandler(factTypeRequestResolver, securityContext, objectFactDao);
  }

  @Test
  public void testIsRetractedWithNullInput() {
    assertFalse(handler.isRetracted(null));
  }

  @Test
  public void testIsRetractedWithoutRetractedHintNotRetracted() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());

    assertFalse(handler.isRetracted(fact));

    verifyNoMoreInteractions(objectFactDao);
  }

  @Test
  public void testIsRetractedWithRetractedHintTrue() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(list(retraction).iterator());

    assertTrue(handler.isRetracted(fact));

    verify(objectFactDao).retrieveMetaFacts(fact.getId());
    verify(objectFactDao).retrieveMetaFacts(retraction.getId());
    verifyNoMoreInteractions(objectFactDao);
  }

  @Test
  public void testIsRetractedWithRetractedRetraction() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction1 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    FactRecord retraction2 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);

    // fact ---> retraction1 ---> retraction2
    // retraction2 cancels out retraction1, thus, fact in not retracted.
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(list(retraction1).iterator());
    when(objectFactDao.retrieveMetaFacts(retraction1.getId())).thenReturn(list(retraction2).iterator());

    assertFalse(handler.isRetracted(fact));
  }

  @Test
  public void testIsRetractedWithRetractedRetractionTwoLevels() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction1 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    FactRecord retraction2 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    FactRecord retraction3 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);

    // fact ---> retraction1 ---> retraction2 ---> retraction3
    // retraction3 cancels out retraction2, thus, fact is retracted because retraction1 holds.
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(list(retraction1).iterator());
    when(objectFactDao.retrieveMetaFacts(retraction1.getId())).thenReturn(list(retraction2).iterator());
    when(objectFactDao.retrieveMetaFacts(retraction2.getId())).thenReturn(list(retraction3).iterator());

    assertTrue(handler.isRetracted(fact));
  }

  @Test
  public void testIsRetractedWithRetractedRetractionComplexTree() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction1 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    FactRecord retraction2 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    FactRecord retraction3 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    FactRecord retraction4 = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);

    // fact -----------> retraction1 ---> retraction2
    //     |-----------> retraction3
    //     |-----------> retraction4
    // retraction2 cancels out retraction1, thus, fact is retracted because of retraction3/retraction4.
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(list(retraction1, retraction3, retraction4).iterator());
    when(objectFactDao.retrieveMetaFacts(retraction1.getId())).thenReturn(list(retraction2).iterator());

    assertTrue(handler.isRetracted(fact));
  }

  @Test
  public void testIsRetractedCachesResult() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(list(retraction).iterator());

    assertTrue(handler.isRetracted(fact));

    verify(objectFactDao).retrieveMetaFacts(fact.getId());
    verify(objectFactDao).retrieveMetaFacts(retraction.getId());
    verifyNoMoreInteractions(objectFactDao);

    assertTrue(handler.isRetracted(fact));
    verifyNoMoreInteractions(objectFactDao);
  }

  @Test
  public void testIsRetractedFiltersNonRetractionFacts() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord meta = new FactRecord().setId(UUID.randomUUID()).setTypeID(UUID.randomUUID());
    FactRecord retraction = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);

    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(list(meta, retraction).iterator());

    assertTrue(handler.isRetracted(fact));

    verify(objectFactDao).retrieveMetaFacts(fact.getId());
    verify(objectFactDao).retrieveMetaFacts(retraction.getId());
    verifyNoMoreInteractions(objectFactDao);
  }

  @Test
  public void testIsRetractedFiltersNonAccessibleFacts() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);

    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(list(retraction).iterator());
    when(securityContext.hasReadPermission(retraction)).thenReturn(false);

    assertFalse(handler.isRetracted(fact));

    verify(securityContext).hasReadPermission(retraction);
  }
}
