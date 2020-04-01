package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.SearchOriginRequest;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.OriginResponseConverter;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
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
  private TiSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResponseConverter originResponseConverter;

  private OriginSearchDelegate delegate;

  @Before
  public void setUp() {
    initMocks(this);

    when(originManager.fetchOrigins()).thenReturn(createEntities());
    when(securityContext.hasReadPermission(isA(OriginEntity.class))).thenReturn(true);

    delegate = new OriginSearchDelegate(securityContext, originManager, originResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testSearchOriginWithoutGeneralViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewOrigins);
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
    entities.add(new OriginEntity().setId(UUID.randomUUID()));
    entities.add(new OriginEntity().setId(UUID.randomUUID()).addFlag(OriginEntity.Flag.Deleted));
    entities.add(new OriginEntity().setId(UUID.randomUUID()));
    return entities;
  }
}
