package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchObjectRequestConverter;
import no.mnemonic.act.platform.service.ti.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.ObjectTypeByIdResponseResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectSearchDelegateTest {

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

  private ObjectSearchDelegate delegate;

  @Before
  public void setup() throws Exception {
    initMocks(this);
    // Mocks required for ElasticSearch access control.
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(SetUtils.set(UUID.randomUUID()));

    // Mocks required for request converter.
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setLimit(25)
            .setCurrentUserID(UUID.randomUUID())
            .setAvailableOrganizationID(Collections.singleton(UUID.randomUUID()))
            .build());


    delegate = new ObjectSearchDelegate(
            securityContext,
            objectFactDao,
            requestConverter,
            factTypeConverter,
            objectTypeConverter
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchObjectsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelFact);
    delegate.handle(new SearchObjectRequest());
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
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
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
