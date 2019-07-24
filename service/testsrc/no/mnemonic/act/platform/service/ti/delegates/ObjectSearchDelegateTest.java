package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsResult;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class ObjectSearchDelegateTest extends AbstractDelegateTest {

  private final UUID objectID = UUID.randomUUID();

  @Mock
  private Function<SearchObjectRequest, FactSearchCriteria> requestConverter;
  @Mock
  private Function<UUID, FactType> factTypeConverter;
  @Mock
  private Function<UUID, ObjectType> objectTypeConverter;

  private ObjectSearchDelegate delegate;

  @Before
  public void setup() {
    // Mocks required for Object search itself.
    when(getFactSearchManager().searchObjects(any())).thenReturn(createSearchResult());
    when(getFactSearchManager().calculateObjectStatistics(any())).thenReturn(ObjectStatisticsResult.builder().build());
    when(getObjectManager().getObjects(any())).thenReturn(SetUtils.set(new ObjectEntity().setId(objectID)).iterator());

    // Mocks required for ElasticSearch access control.
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(SetUtils.set(UUID.randomUUID()));

    // Mocks required for request converter.
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .setAvailableOrganizationID(Collections.singleton(UUID.randomUUID()))
            .build());

    // Mocks required for ObjectConverter.
    when(objectTypeConverter.apply(any())).thenReturn(ObjectType.builder().build());
    when(factTypeConverter.apply(any())).thenReturn(FactType.builder().build());

    // initMocks() will be called by base class.
    delegate = new ObjectSearchDelegate(
            getSecurityContext(),
            getObjectManager(),
            getFactSearchManager(),
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
  public void testSearchObjectsPopulateCriteria() throws Exception {
    delegate.handle(new SearchObjectRequest().addObjectValue("value"));
    verify(requestConverter).apply(isNotNull());
    verify(getFactSearchManager()).calculateObjectStatistics(argThat(criteria -> {
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
      assertEquals(SetUtils.set(objectID), criteria.getObjectID());
      return true;
    }));
  }

  @Test
  public void testSearchObjectsNoResult() throws Exception {
    when(getFactSearchManager().searchObjects(any())).thenReturn(SearchResult.<ObjectDocument>builder().build());
    ResultSet<Object> result = delegate.handle(new SearchObjectRequest());
    assertEquals(0, result.getCount());
    assertEquals(0, ListUtils.list(result.iterator()).size());

    verify(getFactSearchManager()).searchObjects(any());
    verifyNoMoreInteractions(getFactSearchManager());
    verifyZeroInteractions(getObjectManager());
  }

  @Test
  public void testSearchObjects() throws Exception {
    ResultSet<Object> result = delegate.handle(new SearchObjectRequest());
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());

    verify(getFactSearchManager()).searchObjects(any());
    verify(getFactSearchManager()).calculateObjectStatistics(any());
    verify(getObjectManager()).getObjects(ListUtils.list(objectID));
  }

  private SearchResult<ObjectDocument> createSearchResult() {
    return SearchResult.<ObjectDocument>builder()
            .setLimit(25)
            .setCount(100)
            .addValue(new ObjectDocument().setId(objectID))
            .build();
  }
}
