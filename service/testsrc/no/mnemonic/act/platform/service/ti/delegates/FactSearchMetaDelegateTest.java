package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchMetaFactsRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactSearchMetaDelegateTest {

  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private FactSearchHandler factSearchHandler;
  @Mock
  private SearchMetaFactsRequestConverter requestConverter;
  @Mock
  private TiSecurityContext securityContext;

  private FactSearchMetaDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactSearchMetaDelegate(securityContext, requestConverter, factRequestResolver, factSearchHandler);
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchMetaFactsWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelFact);
    delegate.handle(new SearchMetaFactsRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchMetaFactsNoAccessToFact() throws Exception {
    SearchMetaFactsRequest request = new SearchMetaFactsRequest().setFact(UUID.randomUUID());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

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
    verify(securityContext).checkReadPermission(isA(FactRecord.class));
  }

  private void mockSearchMetaFacts() throws Exception {
    when(factRequestResolver.resolveFact(any())).thenReturn(new FactRecord());

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
