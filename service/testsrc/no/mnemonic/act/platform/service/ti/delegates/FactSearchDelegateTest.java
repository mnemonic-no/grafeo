package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchFactRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactSearchDelegateTest {

  @Mock
  private FactSearchHandler factSearchHandler;
  @Mock
  private SearchFactRequestConverter requestConverter;
  @Mock
  private TiSecurityContext securityContext;

  private FactSearchDelegate delegate;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setKeywords("Hello World!")
            .setLimit(25)
            .setCurrentUserID(UUID.randomUUID())
            .setAvailableOrganizationID(Collections.singleton(UUID.randomUUID()))
            .build());
    when(factSearchHandler.search(any(), any())).thenReturn(StreamingResultSet.<Fact>builder()
            .setLimit(25)
            .setCount(100)
            .setValues(Collections.singleton(Fact.builder().build()))
            .build()
    );

    delegate = new FactSearchDelegate(securityContext, requestConverter, factSearchHandler);
  }

  @Test
  public void testSearchFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelFact);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchFactRequest()));
  }

  @Test
  public void testSearchFactsUnboundedRequest() throws Exception {
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setLimit(25)
            .setCurrentUserID(UUID.randomUUID())
            .setAvailableOrganizationID(Collections.singleton(UUID.randomUUID()))
            .build());
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchFactRequest()));
  }

  @Test
  public void testSearchFacts() throws Exception {
    ResultSet<Fact> result = delegate.handle(new SearchFactRequest().setIncludeRetracted(true));
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());

    verify(requestConverter).apply(isNotNull());
    verify(factSearchHandler).search(isNotNull(), eq(true));
  }
}
