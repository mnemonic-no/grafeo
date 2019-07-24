package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class FactSearchMetaDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactSearchHandler factSearchHandler;
  @Mock
  private Function<SearchMetaFactsRequest, FactSearchCriteria> requestConverter;

  private FactSearchMetaDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactSearchMetaDelegate(getSecurityContext(), requestConverter, factSearchHandler);
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchMetaFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    delegate.handle(new SearchMetaFactsRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testSearchMetaFactsNonExistingFact() throws Exception {
    delegate.handle(new SearchMetaFactsRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchMetaFactsNoAccessToFact() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID());
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactEntity.class));

    delegate.handle(request);
  }

  @Test
  public void testSearchMetaFacts() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID()).setIncludeRetracted(true);

    mockSearchMetaFacts();

    ResultSet<Fact> result = delegate.handle(request);
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());

    verify(requestConverter).apply(isNotNull());
    verify(factSearchHandler).search(isNotNull(), eq(true));
    verify(getSecurityContext()).checkReadPermission(isA(FactEntity.class));
  }

  private void mockSearchMetaFacts() {
    when(getFactManager().getFact(any())).thenReturn(new FactEntity());

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
