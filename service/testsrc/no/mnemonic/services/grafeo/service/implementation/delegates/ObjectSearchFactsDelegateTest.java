package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.request.SearchObjectFactsRequestConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactSearchHandler;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private ObjectSearchFactsDelegate delegate;

  @Test
  public void testSearchObjectFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoFact);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchObjectFactsRequest()));
  }

  @Test
  public void testSearchObjectFactsWithoutSpecifiedObject() {
    assertThrows(IllegalArgumentException.class, () -> delegate.handle(new SearchObjectFactsRequest()));
  }

  @Test
  public void testSearchObjectFactsWithNonExistingObjectType() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");

    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeExists(request.getObjectType(), "objectType");

    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testSearchObjectFactsByNonExistingId() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectID(UUID.randomUUID());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
    verify(objectFactDao).getObject(request.getObjectID());
  }

  @Test
  public void testSearchObjectFactsByNonExistingTypeValue() throws Exception {
    SearchObjectFactsRequest request = new SearchObjectFactsRequest().setObjectType("type").setObjectValue("value");
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
    verify(objectTypeHandler).assertObjectTypeExists(request.getObjectType(), "objectType");
    verify(objectFactDao).getObject(request.getObjectType(), request.getObjectValue());
  }

  @Test
  public void testSearchObjectFactsByIdPopulateCriteria() throws Exception {
    when(objectFactDao.getObject(any())).thenReturn(new ObjectRecord().setId(UUID.randomUUID()));

    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectID(UUID.randomUUID());
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValuePopulateCriteria() throws Exception {
    when(objectFactDao.getObject(any(), any())).thenReturn(new ObjectRecord().setId(UUID.randomUUID()));

    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectType("type")
            .setObjectValue("value");
    testPopulateCriteria(request);
  }

  @Test
  public void testSearchObjectFactsById() throws Exception {
    when(objectFactDao.getObject(any())).thenReturn(new ObjectRecord().setId(UUID.randomUUID()));

    SearchObjectFactsRequest request = new SearchObjectFactsRequest()
            .setObjectID(UUID.randomUUID())
            .setIncludeRetracted(true);
    testSearchObjectFacts(request);
  }

  @Test
  public void testSearchObjectFactsByTypeValue() throws Exception {
    when(objectFactDao.getObject(any(), any())).thenReturn(new ObjectRecord().setId(UUID.randomUUID()));

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
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(UUID.randomUUID())
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build())
            .setIndexSelectCriteria(IndexSelectCriteria.builder().build())
            .build());
    when(factSearchHandler.search(any(), any())).thenReturn(StreamingResultSet.<Fact>builder()
            .setLimit(25)
            .setCount(100)
            .setValues(Collections.singleton(Fact.builder().build()))
            .build()
    );
  }
}
