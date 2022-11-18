package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectSearchRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchObjectRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.TraverseGraphHandler;
import no.mnemonic.act.platform.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TraverseByObjectSearchDelegateTest {

  private final AccessControlCriteria accessControlCriteria = AccessControlCriteria.builder()
          .addCurrentUserIdentity(UUID.randomUUID())
          .addAvailableOrganizationID(UUID.randomUUID())
          .build();
  private final IndexSelectCriteria indexSelectCriteria = IndexSelectCriteria.builder().build();

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private TraverseGraphHandler traverseGraphHandler;
  @Mock
  private SearchObjectRequestConverter requestConverter;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private AccessControlCriteriaResolver accessControlCriteriaResolver;
  @Mock
  private IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  private TraverseByObjectSearchDelegate delegate;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    when(accessControlCriteriaResolver.get()).thenReturn(accessControlCriteria);
    when(indexSelectCriteriaResolver.validateAndCreateCriteria(any(), any())).thenReturn(indexSelectCriteria);

    // Mocks required for request converter.
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setLimit(25)
            .setAccessControlCriteria(accessControlCriteria)
            .setIndexSelectCriteria(indexSelectCriteria)
            .build());

    delegate = new TraverseByObjectSearchDelegate(
            securityContext,
            traverseGraphHandler,
            requestConverter,
            objectFactDao,
            accessControlCriteriaResolver,
            indexSelectCriteriaResolver);
  }

  @Test
  public void testTraverseGraphByObjectSearchWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.traverseThreatIntelFact);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new TraverseGraphByObjectSearchRequest()));
  }

  @Test
  public void testTraverseGraphByObjectSearchWithoutSearchResult() throws Exception {
    when(objectFactDao.searchObjects(any())).thenReturn(ResultContainer.<ObjectRecord>builder().build());

    ResultSet<?> result = delegate.handle(new TraverseGraphByObjectSearchRequest().setSearch(new SearchObjectRequest()));
    assertFalse(result.iterator().hasNext());

    verify(requestConverter).apply(notNull());
    verify(objectFactDao).searchObjects(notNull());
    verifyNoInteractions(traverseGraphHandler);
  }

  @Test
  public void testTraverseWithSearch() throws Exception {
    UUID objectId1 = UUID.randomUUID();
    UUID objectId2 = UUID.randomUUID();
    String query = "g.outE()";


    TraverseGraphByObjectSearchRequest request = new TraverseGraphByObjectSearchRequest()
            .setSearch(new SearchObjectRequest())
            .setTraverse(new TraverseGraphRequest().setQuery(query));

    Iterator<ObjectRecord> searchResult = set(new ObjectRecord().setId(objectId1), new ObjectRecord().setId(objectId2)).iterator();
    when(objectFactDao.searchObjects(any())).thenReturn(ResultContainer.<ObjectRecord>builder().setValues(searchResult).build());

    delegate.handle(request);

    verify(indexSelectCriteriaResolver).validateAndCreateCriteria(isNull(), isNull());
    verify(traverseGraphHandler).traverse(
            eq(set(objectId1, objectId2)),
            eq(query),
            argThat(traverseParams -> {
              assertNull(traverseParams.getBaseSearchCriteria().getStartTimestamp());
              assertNull(traverseParams.getBaseSearchCriteria().getEndTimestamp());
              assertNotNull(traverseParams.getBaseSearchCriteria().getAccessControlCriteria());
              assertNotNull(traverseParams.getBaseSearchCriteria().getIndexSelectCriteria());
              assertFalse(traverseParams.isIncludeRetracted());
              assertEquals(25, traverseParams.getLimit());
              return true;
            }));
  }

  @Test
  public void testTraverseWithSearchAndParams() throws Exception {
    UUID objectId1 = UUID.randomUUID();
    UUID objectId2 = UUID.randomUUID();
    String query = "g.outE()";
    Long startTimestamp = 1L;
    Long endTimestamp = 2L;

    TraverseGraphByObjectSearchRequest request = new TraverseGraphByObjectSearchRequest()
            .setSearch(new SearchObjectRequest())
            .setTraverse(new TraverseGraphRequest()
                    .setQuery(query)
                    .setIncludeRetracted(true)
                    .setStartTimestamp(startTimestamp)
                    .setEndTimestamp(endTimestamp)
                    .setLimit(10)
            );

    Iterator<ObjectRecord> searchResult = set(new ObjectRecord().setId(objectId1), new ObjectRecord().setId(objectId2)).iterator();
    when(objectFactDao.searchObjects(any())).thenReturn(ResultContainer.<ObjectRecord>builder().setValues(searchResult).build());

    delegate.handle(request);

    verify(indexSelectCriteriaResolver).validateAndCreateCriteria(startTimestamp, endTimestamp);
    verify(traverseGraphHandler).traverse(
            eq(set(objectId1, objectId2)),
            eq(query),
            argThat(traverseParams -> {
              assertEquals(startTimestamp, traverseParams.getBaseSearchCriteria().getStartTimestamp());
              assertEquals(endTimestamp, traverseParams.getBaseSearchCriteria().getEndTimestamp());
              assertNotNull(traverseParams.getBaseSearchCriteria().getAccessControlCriteria());
              assertNotNull(traverseParams.getBaseSearchCriteria().getIndexSelectCriteria());
              assertTrue(traverseParams.isIncludeRetracted());
              assertEquals(10, traverseParams.getLimit());
              return true;
            }));
  }
}
