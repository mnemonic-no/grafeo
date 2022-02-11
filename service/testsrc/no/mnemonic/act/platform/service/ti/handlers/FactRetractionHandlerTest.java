package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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
    when(objectFactDao.retrieveMetaFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());

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
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(createResultContainer(retraction));

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
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(createResultContainer(retraction1));
    when(objectFactDao.retrieveMetaFacts(retraction1.getId())).thenReturn(createResultContainer(retraction2));

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
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(createResultContainer(retraction1));
    when(objectFactDao.retrieveMetaFacts(retraction1.getId())).thenReturn(createResultContainer(retraction2));
    when(objectFactDao.retrieveMetaFacts(retraction2.getId())).thenReturn(createResultContainer(retraction3));

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
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(createResultContainer(retraction1, retraction3, retraction4));
    when(objectFactDao.retrieveMetaFacts(retraction1.getId())).thenReturn(createResultContainer(retraction2));

    assertTrue(handler.isRetracted(fact));
  }

  @Test
  public void testIsRetractedCachesResult() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);
    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(createResultContainer(retraction));

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

    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(createResultContainer(meta, retraction));

    assertTrue(handler.isRetracted(fact));

    verify(objectFactDao).retrieveMetaFacts(fact.getId());
    verify(objectFactDao).retrieveMetaFacts(retraction.getId());
    verifyNoMoreInteractions(objectFactDao);
  }

  @Test
  public void testIsRetractedFiltersNonAccessibleFacts() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID()).setFlags(set(FactRecord.Flag.RetractedHint));
    FactRecord retraction = new FactRecord().setId(UUID.randomUUID()).setTypeID(retractionFactTypeID);

    when(objectFactDao.retrieveMetaFacts(fact.getId())).thenReturn(createResultContainer(retraction));
    when(securityContext.hasReadPermission(retraction)).thenReturn(false);

    assertFalse(handler.isRetracted(fact));

    verify(securityContext).hasReadPermission(retraction);
  }

  private ResultContainer<FactRecord> createResultContainer(FactRecord... fact) {
    return ResultContainer.<FactRecord>builder()
            .setValues(list(fact).iterator())
            .build();
  }
}
