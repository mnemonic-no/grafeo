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
import no.mnemonic.act.platform.service.ti.converters.FactTypeByIdConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeByIdConverter;
import no.mnemonic.act.platform.service.ti.converters.SearchObjectRequestConverter;
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

public class ObjectSearchDelegateTest extends AbstractDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private SearchObjectRequestConverter requestConverter;
  @Mock
  private FactTypeByIdConverter factTypeConverter;
  @Mock
  private ObjectTypeByIdConverter objectTypeConverter;

  private ObjectSearchDelegate delegate;

  @Before
  public void setup() throws Exception {
    // Mocks required for ElasticSearch access control.
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(SetUtils.set(UUID.randomUUID()));

    // Mocks required for request converter.
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setLimit(25)
            .setCurrentUserID(UUID.randomUUID())
            .setAvailableOrganizationID(Collections.singleton(UUID.randomUUID()))
            .build());

    // initMocks() will be called by base class.
    delegate = new ObjectSearchDelegate(
            getSecurityContext(),
            objectFactDao,
            requestConverter,
            factTypeConverter,
            objectTypeConverter
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchObjectsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
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
  public void testSearchObjectsSingleBatch() throws Exception {
    int count = 3;
    when(objectFactDao.searchObjects(any())).thenReturn(createSearchResult(count));
    when(objectFactDao.calculateObjectStatistics(any())).thenReturn(ObjectStatisticsContainer.builder().build());

    ResultSet<Object> result = delegate.handle(new SearchObjectRequest());
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
  public void testSearchObjectsMultipleBatches() throws Exception {
    int count = 1001;
    when(objectFactDao.searchObjects(any())).thenReturn(createSearchResult(count));
    when(objectFactDao.calculateObjectStatistics(any())).thenReturn(ObjectStatisticsContainer.builder().build());

    ResultSet<Object> result = delegate.handle(new SearchObjectRequest());
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
