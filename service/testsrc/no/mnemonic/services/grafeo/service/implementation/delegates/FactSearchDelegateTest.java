package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.request.v1.SearchFactRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.request.SearchFactRequestConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactSearchHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactSearchDelegateTest {

  private final AccessControlCriteria accessControlCriteria = AccessControlCriteria.builder()
          .addCurrentUserIdentity(UUID.randomUUID())
          .addAvailableOrganizationID(UUID.randomUUID())
          .build();
  private final IndexSelectCriteria indexSelectCriteria = IndexSelectCriteria.builder().build();

  @Mock
  private FactSearchHandler factSearchHandler;
  @Mock
  private SearchFactRequestConverter requestConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private FactSearchDelegate delegate;

  @Test
  public void testSearchFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoFact);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchFactRequest()));
  }

  @Test
  public void testSearchFactsUnboundedRequest() throws Exception {
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setLimit(25)
            .setAccessControlCriteria(accessControlCriteria)
            .setIndexSelectCriteria(indexSelectCriteria)
            .build());
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchFactRequest()));
  }

  @Test
  public void testSearchFacts() throws Exception {
    when(requestConverter.apply(any())).thenReturn(FactSearchCriteria.builder()
            .setKeywords("Hello World!")
            .setLimit(25)
            .setAccessControlCriteria(accessControlCriteria)
            .setIndexSelectCriteria(indexSelectCriteria)
            .build());
    when(factSearchHandler.search(any(), any())).thenReturn(StreamingResultSet.<Fact>builder()
            .setLimit(25)
            .setCount(100)
            .setValues(Collections.singleton(Fact.builder().build()))
            .build()
    );

    ResultSet<Fact> result = delegate.handle(new SearchFactRequest().setIncludeRetracted(true));
    assertEquals(25, result.getLimit());
    assertEquals(100, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());

    verify(requestConverter).apply(isNotNull());
    verify(factSearchHandler).search(isNotNull(), eq(true));
  }
}
