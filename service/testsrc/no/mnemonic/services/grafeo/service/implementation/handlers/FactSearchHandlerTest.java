package no.mnemonic.services.grafeo.service.implementation.handlers;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.result.ResultContainer;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactResponseConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactSearchHandlerTest {

  @Mock
  private FactRetractionHandler retractionHandler;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private FactResponseConverter factResponseConverter;

  private FactSearchHandler handler;

  @Before
  public void setUp() {
    initMocks(this);

    when(factResponseConverter.apply(notNull())).thenReturn(Fact.builder().build());
    when(securityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(true);

    handler = new FactSearchHandler(retractionHandler, objectFactDao, securityContext, factResponseConverter);
  }

  @Test
  public void testSearchFactsWithLimit() {
    mockSearch(1);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(1, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());
  }

  @Test
  public void testSearchFactsWithoutLimit() throws Exception {
    mockSearch(1);

    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.unlimitedThreatIntelSearch);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(0));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(10000, result.getLimit());
    assertEquals(1, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());
  }

  @Test
  public void testSearchFactsWithLimitAboveMaxLimit() throws Exception {
    mockSearch(1);

    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.unlimitedThreatIntelSearch);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(10001));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(10000, result.getLimit());
    assertEquals(1, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());
  }

  @Test
  public void testSearchFactsUnlimited() throws Exception {
    mockSearch(10001);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(0));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(0, result.getLimit());
    assertEquals(10001, result.getCount());
    assertEquals(10001, ListUtils.list(result.iterator()).size());

    verify(securityContext).checkPermission(FunctionConstants.unlimitedThreatIntelSearch);
  }

  @Test
  public void testSearchFactsNoResults() {
    mockSearch(0);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(0, result.getCount());
    assertEquals(0, ListUtils.list(result.iterator()).size());
  }

  @Test
  public void testSearchFactsFiltersNonAccessibleFacts() {
    mockSearch(3);
    when(securityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(true, false, true);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(3, result.getCount());
    assertEquals(2, ListUtils.list(result.iterator()).size());

    verify(securityContext, times(3)).hasReadPermission(isA(FactRecord.class));
    verify(factResponseConverter, times(2)).apply(isA(FactRecord.class));
  }

  @Test
  public void testSearchFactsFetchesUntilLimit() {
    mockSearch(100);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(25));
    ResultSet<Fact> result = handler.search(criteria, null);

    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(25, ListUtils.list(result.iterator()).size());

    verify(securityContext, times(25)).hasReadPermission(isA(FactRecord.class));
    verify(factResponseConverter, times(25)).apply(isA(FactRecord.class));
  }

  @Test
  public void testSearchFactsIncludeRetracted() {
    FactRecord fact = new FactRecord().addFlag(FactRecord.Flag.RetractedHint);

    mockSearch(fact);
    when(retractionHandler.isRetracted(fact)).thenReturn(true);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    ResultSet<Fact> result = handler.search(criteria, true);

    assertEquals(1, ListUtils.list(result.iterator()).size());
    verify(retractionHandler).isRetracted(fact);
  }

  @Test
  public void testSearchFactsExcludeRetractedNotRetracted() {
    FactRecord fact = new FactRecord();

    mockSearch(fact);
    when(retractionHandler.isRetracted(fact)).thenReturn(false);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    ResultSet<Fact> result = handler.search(criteria, false);

    assertEquals(1, ListUtils.list(result.iterator()).size());
    verify(retractionHandler).isRetracted(fact);
  }

  @Test
  public void testSearchFactsExcludeRetracted() {
    FactRecord fact = new FactRecord().addFlag(FactRecord.Flag.RetractedHint);

    mockSearch(fact);
    when(retractionHandler.isRetracted(fact)).thenReturn(true);

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    ResultSet<Fact> result = handler.search(criteria, false);

    assertEquals(0, ListUtils.list(result.iterator()).size());
    verify(retractionHandler).isRetracted(fact);
  }

  private void mockSearch(int count) {
    List<FactRecord> records = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      records.add(new FactRecord());
    }

    when(objectFactDao.searchFacts(notNull())).thenReturn(ResultContainer.<FactRecord>builder()
            .setCount(count)
            .setValues(records.iterator())
            .build());
  }

  private void mockSearch(FactRecord fact) {
    when(objectFactDao.searchFacts(notNull())).thenReturn(ResultContainer.<FactRecord>builder()
            .setCount(1)
            .setValues(ListUtils.list(fact).iterator())
            .build());
  }

  private FactSearchCriteria createFactSearchCriteria(ObjectPreparation<FactSearchCriteria.Builder> preparation) {
    FactSearchCriteria.Builder builder = FactSearchCriteria.builder()
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(UUID.randomUUID())
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build())
            .setIndexSelectCriteria(IndexSelectCriteria.builder().build());
    if (preparation != null) {
      builder = preparation.prepare(builder);
    }
    return builder.build();
  }

  private interface ObjectPreparation<T> {
    T prepare(T e);
  }
}
