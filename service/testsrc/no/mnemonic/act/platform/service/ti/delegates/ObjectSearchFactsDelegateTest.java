package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.converters.SearchObjectFactsRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ObjectSearchFactsDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactSearchHandler factSearchHandler;
  @Mock
  private SearchObjectFactsRequestConverter requestConverter;

  private ObjectSearchFactsDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new ObjectSearchFactsDelegate(getSecurityContext(), getObjectManager(), requestConverter, factSearchHandler);
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchObjectFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    delegate.handle(new SearchObjectFactsRequest());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSearchObjectFactsWithoutSpecifiedObject() throws Exception {
    delegate.handle(new SearchObjectFactsRequest());
  }

  @Test
  public void testSearchObjectFactsWithNonExistingObjectType() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");

    try {
      delegate.handle(request);
      fail();
    } catch (InvalidArgumentException ignored) {
      verify(getObjectManager()).getObjectType(request.getObjectType());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testSearchObjectFactsByNonExistingId() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObject(request.getObjectID());
      verifyNoMoreInteractions(getObjectManager());
    }
  }

  @Test
  public void testSearchObjectFactsByNonExistingTypeValue() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");
    when(getObjectManager().getObjectType(request.getObjectType())).thenReturn(new ObjectTypeEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission((ObjectEntity) isNull());

    try {
      delegate.handle(request);
      fail();
    } catch (AccessDeniedException ignored) {
      verify(getObjectManager()).getObjectType(request.getObjectType());
      verify(getObjectManager()).getObject(request.getObjectType(), request.getObjectValue());
      verifyNoMoreInteractions(getObjectManager());
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
    verify(getSecurityContext()).checkReadPermission(isA(ObjectEntity.class));
  }

  private void mockSearchObjectFacts() throws Exception {
    when(getObjectManager().getObjectType(isA(String.class))).thenReturn(new ObjectTypeEntity());
    when(getObjectManager().getObject(any())).thenReturn(new ObjectEntity().setId(UUID.randomUUID()));
    when(getObjectManager().getObject(any(), any())).thenReturn(new ObjectEntity().setId(UUID.randomUUID()));

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
