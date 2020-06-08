package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchObjectFactsRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectSearchFactsDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactSearchHandler factSearchHandler;
  @Mock
  private SearchObjectFactsRequestConverter requestConverter;
  @Mock
  private ObjectTypeHandler objectTypeHandler;
  @Mock
  private TiSecurityContext securityContext;

  private ObjectSearchFactsDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new ObjectSearchFactsDelegate(
      securityContext,
      objectFactDao,
      requestConverter,
      factSearchHandler,
      objectTypeHandler);
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchObjectFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelFact);
    delegate.handle(new SearchObjectFactsRequest());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchObjectFactsWithoutSpecifiedObject() throws Exception {
    delegate.handle(new SearchObjectFactsRequest());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testSearchObjectFactsWithNonExistingObjectType() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");

    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeExists(request.getObjectType(), "objectType");

    delegate.handle(request);
  }

  @Test
  public void testSearchObjectFactsByNonExistingId() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(objectFactDao).getObject(request.getObjectID());
    }
  }

  @Test
  public void testSearchObjectFactsByNonExistingTypeValue() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(objectTypeHandler).assertObjectTypeExists(request.getObjectType(), "objectType");
      verify(objectFactDao).getObject(request.getObjectType(), request.getObjectValue());
    }
  }

  @Test
  public void testSearchObjectFactsByIdPopulateCriteria() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectID(UUID.randomUUID());
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValuePopulateCriteria() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectType("type")
            .setObjectValue("value");
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsById() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectID(UUID.randomUUID())
            .setIncludeRetracted(true);
    testSearchObjectFacts(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValue() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectType("type")
            .setObjectValue("value")
            .setIncludeRetracted(true);
    testSearchObjectFacts(request);
  }

  private void testPopulateCriteria(SearchObjectFactsRequest request) throws Exception {
    mockSearchObjectFacts();
    delegate.handle(request);
    verify(requestConverter).apply(argThat(req -> {
      assertNull(req.getObjectType());
      assertNull(req.getObjectValue());
      assertNotNull(req.getObjectID());
      return true;
    }));
  }

  private void testSearchObjectFacts(SearchObjectFactsRequest request) throws Exception {
    mockSearchObjectFacts();

    ResultSet<Fact> result = delegate.handle(request);
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());

    verify(requestConverter).apply(isNotNull());
    verify(factSearchHandler).search(isNotNull(), eq(request.getIncludeRetracted()));
    verify(securityContext).checkReadPermission(isA(ObjectRecord.class));
  }

  private void mockSearchObjectFacts() throws Exception {
    when(objectFactDao.getObject(any())).thenReturn(new ObjectRecord().setId(UUID.randomUUID()));
    when(objectFactDao.getObject(any(), any())).thenReturn(new ObjectRecord().setId(UUID.randomUUID()));

    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .setAvailableOrganizationID(Collections.singleton(UUID.randomUUID()))
            .build());
    when(factSearchHandler.search(any(), any())).thenReturn(StreamingResultSet.<Fact>builder()
            .setLimit(25)
            .setCount(100)
            .setValues(Collections.singleton(Fact.builder().build()))
            .build()
    );
  }
}
