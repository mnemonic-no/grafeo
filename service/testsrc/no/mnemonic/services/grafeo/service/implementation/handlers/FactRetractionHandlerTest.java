package no.mnemonic.services.grafeo.service.implementation.handlers;

import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactRetractionHandlerTest {

  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private ObjectFactDao objectFactDao;
  @InjectMocks
  private FactRetractionHandler handler;

  private final UUID retractionFactTypeID = UUID.randomUUID();

  @BeforeEach
  public void setUp() {
    // Common mocks used by most tests.
    lenient().when(factTypeRequestResolver.resolveRetractionFactType()).thenReturn(new FactTypeEntity().setId(retractionFactTypeID));
    lenient().when(securityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(true);
    lenient().when(objectFactDao.retrieveMetaFacts(any())).thenReturn(Collections.emptyIterator());
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
