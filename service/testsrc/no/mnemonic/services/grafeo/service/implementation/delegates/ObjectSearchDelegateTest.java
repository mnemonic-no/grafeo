package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectRequest;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.services.grafeo.dao.api.result.ResultContainer;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.request.SearchObjectRequestConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.ObjectTypeByIdResponseResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
  private GrafeoSecurityContext securityContext;
  @Mock
  private AccessControlCriteriaResolver accessControlCriteriaResolver;
  @InjectMocks
  private ObjectSearchDelegate delegate;

  @BeforeEach
  public void setup() throws Exception {
    // Mocks required for request converter.
    lenient().when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setKeywords("Hello World!")
            .setLimit(25)
            .setAccessControlCriteria(accessControlCriteria)
            .setIndexSelectCriteria(indexSelectCriteria)
            .build());
  }

  @Test
  public void testSearchObjectsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoFact);
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
    when(accessControlCriteriaResolver.get()).thenReturn(accessControlCriteria);

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
    when(accessControlCriteriaResolver.get()).thenReturn(accessControlCriteria);

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
    when(accessControlCriteriaResolver.get()).thenReturn(accessControlCriteria);

    SearchObjectRequest request = new SearchObjectRequest()
            .setIncludeStatistics(true)
            .setStartTimestamp(11111L)
            .setEndTimestamp(22222L);
    ResultSet<Object> result = delegate.handle(request);
    assertEquals(count, result.getCount());
    assertEquals(count, ListUtils.list(result.iterator()).size());

    verify(objectFactDao).searchObjects(notNull());
    verify(objectFactDao).calculateObjectStatistics(argThat(criteria -> {
      assertEquals(request.getStartTimestamp(), criteria.getStartTimestamp());
      assertEquals(request.getEndTimestamp(), criteria.getEndTimestamp());
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
