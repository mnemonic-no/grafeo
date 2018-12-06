package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FactSearchMetaDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactSearchHandler factSearchHandler;

  private FactSearchMetaDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = FactSearchMetaDelegate.builder()
            .setFactSearchHandler(factSearchHandler)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactSearchHandler() {
    FactSearchMetaDelegate.builder().build();
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
  public void testSearchMetaFactsPopulateCriteria() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest()
            .setFact(UUID.randomUUID())
            .addFactValue("factValue")
            .setIncludeRetracted(true);

    mockSearchMetaFacts();

    delegate.handle(request);

    verify(factSearchHandler).search(argThat(criteria -> {
      assertTrue(criteria.getInReferenceTo().size() > 0);
      assertTrue(criteria.getFactValue().size() > 0);
      assertTrue(criteria.getAvailableOrganizationID().size() > 0);
      assertNotNull(criteria.getCurrentUserID());
      return true;
    }), eq(request.getIncludeRetracted()));
  }

  @Test
  public void testSearchMetaFacts() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID());

    mockSearchMetaFacts();

    ResultSet<Fact> result = delegate.handle(request);
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, result.getValues().size());

    verify(factSearchHandler).search(isNotNull(), isNull());
    verify(getSecurityContext()).checkReadPermission(isA(FactEntity.class));
  }

  private void mockSearchMetaFacts() {
    when(getFactManager().getFact(any())).thenReturn(new FactEntity());

    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));

    when(factSearchHandler.search(any(), any())).thenReturn(ResultSet.<Fact>builder()
            .setLimit(25)
            .setCount(100)
            .setValues(Collections.singleton(Fact.builder().build()))
            .build()
    );
  }

}
