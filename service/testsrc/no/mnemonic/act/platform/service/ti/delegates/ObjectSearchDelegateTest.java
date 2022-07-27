package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchObjectRequestConverter;
import no.mnemonic.act.platform.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.ObjectTypeByIdResponseResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectSearchDelegateTest {

  private final AccessControlCriteria accessControlCriteria = AccessControlCriteria.builder()
          .addCurrentUserIdentity(UUID.randomUUID())
          .addAvailableOrganizationID(UUID.randomUUID())
          .build();
  private final IndexSelectCriteria indexSelectCriteria = IndexSelectCriteria.builder().build();

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private SearchObjectRequestConverter requestConverter;
  @Mock
  private FactTypeByIdResponseResolver factTypeConverter;
  @Mock
  private ObjectTypeByIdResponseResolver objectTypeConverter;
  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private AccessControlCriteriaResolver accessControlCriteriaResolver;

  private ObjectSearchDelegate delegate;

  @Before
  public void setup() throws Exception {
    initMocks(this);
    // Mocks required for ElasticSearch access control.
    when(accessControlCriteriaResolver.get()).thenReturn(accessControlCriteria);

    // Mocks required for request converter.
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setKeywords("Hello World!")
            .setLimit(25)
            .setAccessControlCriteria(accessControlCriteria)
            .setIndexSelectCriteria(indexSelectCriteria)
            .build());

    delegate = new ObjectSearchDelegate(
            securityContext,
            accessControlCriteriaResolver,
            objectFactDao,
            requestConverter,
            factTypeConverter,
            objectTypeConverter
    );
  }

  @Test
  public void testSearchObjectsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelFact);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchObjectRequest()));
  }

  @Test
  public void testSearchObjectsUnboundedRequest() throws Exception {
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setLimit(25)
            .setAccessControlCriteria(accessControlCriteria)
            .setIndexSelectCriteria(indexSelectCriteria)
            .build());
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchObjectRequest()));
  }

  @Test
  public void testSearchObjectsNoResult() throws Exception {
    when(objectFactDao.searchObjects(any())).thenReturn(ResultContainer.<ObjectRecord>builder().build());

    ResultSet<Object> result = delegate.handle(new SearchObjectRequest());
    assertEquals(25, result.getLimit());
    assertEquals(0, result.getCount());
    assertEquals(0, ListUtils.list(result.iterator()).size());

    verify(requestConverter).apply(notNull());
    verify(objectFactDao).searchObjects(notNull());
    verifyNoMoreInteractions(objectFactDao);
  }

  @Test
  public void testSearchObjectsSkipStatistics() throws Exception {
    int count = 3;
    when(objectFactDao.searchObjects(any())).thenReturn(createSearchResult(count));

    ResultSet<Object> result = delegate.handle(new SearchObjectRequest().setIncludeStatistics(false));
    assertEquals(25, result.getLimit());
    assertEquals(count, result.getCount());
    assertEquals(count, ListUtils.list(result.iterator()).size());

    verify(objectFactDao).searchObjects(notNull());
    verify(objectFactDao, never()).calculateObjectStatistics(any());
  }

  @Test
  public void testSearchObjectsSingleBatchIncludeStatistics() throws Exception {
    int count = 3;
    when(objectFactDao.searchObjects(any())).thenReturn(createSearchResult(count));
    when(objectFactDao.calculateObjectStatistics(any())).thenReturn(ObjectStatisticsContainer.builder().build());

    ResultSet<Object> result = delegate.handle(new SearchObjectRequest().setIncludeStatistics(true));
    assertEquals(25, result.getLimit());
    assertEquals(count, result.getCount());
    assertEquals(count, ListUtils.list(result.iterator()).size());

    verify(objectFactDao).searchObjects(notNull());
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertSame(accessControlCriteria, criteria.getAccessControlCriteria());
      assertSame(indexSelectCriteria, criteria.getIndexSelectCriteria());
      assertEquals(count, criteria.getObjectID().size());
      return true;
    }));
  }

  @Test
  public void testSearchObjectsMultipleBatchesIncludeStatistics() throws Exception {
    int count = 1001;
    when(objectFactDao.searchObjects(any())).thenReturn(createSearchResult(count));
    when(objectFactDao.calculateObjectStatistics(any())).thenReturn(ObjectStatisticsContainer.builder().build());

    ResultSet<Object> result = delegate.handle(new SearchObjectRequest().setIncludeStatistics(true));
    assertEquals(count, result.getCount());
    assertEquals(count, ListUtils.list(result.iterator()).size());

    verify(objectFactDao).searchObjects(notNull());
    verify(objectFactDao, times(2)).calculateObjectStatistics(notNull());
  }

  @Test
  public void testSearchObjectsIncludeTimeFilterInStatisticsCriteria() throws Exception {
    int count = 3;
    when(objectFactDao.searchObjects(any())).thenReturn(createSearchResult(count));
    when(objectFactDao.calculateObjectStatistics(any())).thenReturn(ObjectStatisticsContainer.builder().build());

    SearchObjectRequest request = new SearchObjectRequest()
            .setIncludeStatistics(true)
            .setAfter(11111L)
            .setBefore(22222L);
    ResultSet<Object> result = delegate.handle(request);
    assertEquals(count, result.getCount());
    assertEquals(count, ListUtils.list(result.iterator()).size());

    verify(objectFactDao).searchObjects(notNull());
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(request.getAfter(), criteria.getStartTimestamp());
      assertEquals(request.getBefore(), criteria.getEndTimestamp());
      return true;
    }));
  }

  private ResultContainer<ObjectRecord> createSearchResult(int count) {
    List<ObjectRecord> records = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      records.add(new ObjectRecord().setId(UUID.randomUUID()));
    }

    return ResultContainer.<ObjectRecord>builder()
            .setCount(count)
            .setValues(records.iterator())
            .build();
  }
}
