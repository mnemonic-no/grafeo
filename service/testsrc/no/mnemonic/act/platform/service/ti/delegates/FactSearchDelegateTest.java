package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class FactSearchDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactSearchHandler factSearchHandler;

  private FactSearchDelegate delegate;

  @Before
  public void setup() {
    when(getSecurityContext().getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(getSecurityContext().getAvailableOrganizationID()).thenReturn(Collections.singleton(UUID.randomUUID()));
    when(factSearchHandler.search(any(), any())).thenReturn(StreamingResultSet.<Fact>builder()
            .setLimit(25)
            .setCount(100)
            .setValues(Collections.singleton(Fact.builder().build()))
            .build()
    );

    // initMocks() will be called by base class.
    delegate = FactSearchDelegate.builder()
            .setFactSearchHandler(factSearchHandler)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateDelegateWithoutFactSearchHandler() {
    FactSearchDelegate.builder().build();
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    delegate.handle(new SearchFactRequest());
  }

  @Test
  public void testSearchFactsPopulateCriteria() throws Exception {
    delegate.handle(new SearchFactRequest().addFactValue("value").setIncludeRetracted(true));
    verify(factSearchHandler).search(argThat(criteria -> {
      assertNotNull(criteria.getCurrentUserID());
      assertNotNull(criteria.getAvailableOrganizationID());
      assertNotNull(criteria.getFactValue());
      return true;
    }), eq(true));
  }

  @Test
  public void testSearchFacts() throws Exception {
    ResultSet<Fact> result = delegate.handle(new SearchFactRequest());
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());

    verify(factSearchHandler).search(isNotNull(), isNull());
  }

}
