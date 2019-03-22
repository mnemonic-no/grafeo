package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
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

  private ObjectSearchFactsDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = ObjectSearchFactsDelegate.builder()
            .setFactSearchHandler(factSearchHandler)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactSearchHandler() {
    ObjectSearchFactsDelegate.builder().build();
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
            .setObjectID(UUID.randomUUID())
            .addFactValue("factValue")
            .setIncludeRetracted(true);
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValuePopulateCriteria() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectType("type")
            .setObjectValue("value")
            .addFactValue("factValue")
            .setIncludeRetracted(true);
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsById() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(UUID.randomUUID());
    testSearchObjectFacts(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValue() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");
    testSearchObjectFacts(request);
  }

  private void testPopulateCriteria(SearchObjectFactsRequest request) throws Exception {
    mockSearchObjectFacts();
    delegate.handle(request);
    verify(factSearchHandler).search(argThat(criteria -> {
      assertTrue(criteria.getObjectTypeName().size() == 0);
      assertTrue(criteria.getObjectValue().size() == 0);
      assertTrue(criteria.getObjectID().size() > 0);
      assertTrue(criteria.getFactValue().size() > 0);
      assertTrue(criteria.getAvailableOrganizationID().size() > 0);
      assertNotNull(criteria.getCurrentUserID());
      return true;
    }), eq(request.getIncludeRetracted()));
  }

  private void testSearchObjectFacts(SearchObjectFactsRequest request) throws Exception {
    mockSearchObjectFacts();

    ResultSet<Fact> result = delegate.handle(request);
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());

    verify(factSearchHandler).search(isNotNull(), isNull());
    verify(getSecurityContext()).checkReadPermission(isA(ObjectEntity.class));
  }

  private void mockSearchObjectFacts() {
    when(getObjectManager().getObjectType(isA(String.class))).thenReturn(new ObjectTypeEntity());
    when(getObjectManager().getObject(any())).thenReturn(new ObjectEntity().setId(UUID.randomUUID()));
    when(getObjectManager().getObject(any(), any())).thenReturn(new ObjectEntity().setId(UUID.randomUUID()));

    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));

    when(factSearchHandler.search(any(), any())).thenReturn(StreamingResultSet.<Fact>builder()
            .setLimit(25)
            .setCount(100)
            .setValues(Collections.singleton(Fact.builder().build()))
            .build()
    );
  }

}
