package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.request.v1.SearchOriginRequest;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginSearchDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResponseConverter originResponseConverter;

  private OriginSearchDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);

    when(originManager.fetchOrigins()).thenReturn(createEntities());
    when(originResponseConverter.apply(notNull())).thenReturn(Origin.builder().build());
    when(securityContext.hasReadPermission(isA(OriginEntity.class))).thenReturn(true);

    delegate = new OriginSearchDelegate(securityContext, originManager, originResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchOriginWithoutGeneralViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewThreatIntelOrigin);
    delegate.handle(new SearchOriginRequest());
  }

  @Test
  public void testSearchOriginWithDefaultValues() throws Exception {
    ResultSet<Origin> result = delegate.handle(new SearchOriginRequest());

    assertEquals(2, result.getCount());
    assertEquals(25, result.getLimit());
    assertEquals(2, ListUtils.list(result.iterator()).size());
    verify(securityContext, times(2)).hasReadPermission(isA(OriginEntity.class));
    verify(originResponseConverter, times(2)).apply(notNull());
  }

  @Test
  public void testSearchOriginFilterBySingleType() throws Exception {
    ResultSet<Origin> result = delegate.handle(new SearchOriginRequest().addType(SearchOriginRequest.Type.Group));

    assertEquals(1, result.getCount());
    assertEquals(1, ListUtils.list(result.iterator()).size());
    verify(securityContext, times(1)).hasReadPermission(isA(OriginEntity.class));
    verify(originResponseConverter, times(1)).apply(notNull());
  }

  @Test
  public void testSearchOriginFilterByMultipleTypes() throws Exception {
    ResultSet<Origin> result = delegate.handle(new SearchOriginRequest()
            .addType(SearchOriginRequest.Type.Group)
            .addType(SearchOriginRequest.Type.User));

    assertEquals(2, result.getCount());
    assertEquals(2, ListUtils.list(result.iterator()).size());
    verify(securityContext, times(2)).hasReadPermission(isA(OriginEntity.class));
    verify(originResponseConverter, times(2)).apply(notNull());
  }

  @Test
  public void testSearchOriginIncludeDeleted() throws Exception {
    ResultSet<Origin> result = delegate.handle(new SearchOriginRequest().setIncludeDeleted(true));

    assertEquals(3, result.getCount());
    assertEquals(3, ListUtils.list(result.iterator()).size());
    verify(securityContext, times(3)).hasReadPermission(isA(OriginEntity.class));
    verify(originResponseConverter, times(3)).apply(notNull());
  }

  @Test
  public void testSearchOriginWithLimit() throws Exception {
    ResultSet<Origin> result = delegate.handle(new SearchOriginRequest().setLimit(1));

    assertEquals(1, result.getCount());
    assertEquals(1, result.getLimit());
    assertEquals(1, ListUtils.list(result.iterator()).size());
    verify(securityContext, times(1)).hasReadPermission(isA(OriginEntity.class));
    verify(originResponseConverter, times(1)).apply(notNull());
  }

  @Test
  public void testSearchOriginUnlimited() throws Exception {
    ResultSet<Origin> result = delegate.handle(new SearchOriginRequest().setLimit(0));

    assertEquals(2, result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(2, ListUtils.list(result.iterator()).size());
    verify(securityContext, times(2)).hasReadPermission(isA(OriginEntity.class));
    verify(originResponseConverter, times(2)).apply(notNull());
  }

  @Test
  public void testSearchOriginFiltersNonAccessibleOrigins() throws Exception {
    when(securityContext.hasReadPermission(isA(OriginEntity.class))).thenReturn(false);
    ResultSet<Origin> result = delegate.handle(new SearchOriginRequest());

    assertEquals(0, result.getCount());
    assertEquals(0, ListUtils.list(result.iterator()).size());
    verify(securityContext, times(2)).hasReadPermission(isA(OriginEntity.class));
    verify(originResponseConverter, never()).apply(any());
  }

  private List<OriginEntity> createEntities() {
    List<OriginEntity> entities = new ArrayList<>();
    entities.add(new OriginEntity().setId(UUID.randomUUID()).setType(OriginEntity.Type.User));
    entities.add(new OriginEntity().setId(UUID.randomUUID()).setType(OriginEntity.Type.User).addFlag(OriginEntity.Flag.Deleted));
    entities.add(new OriginEntity().setId(UUID.randomUUID()).setType(OriginEntity.Type.Group));
    return entities;
  }
}
