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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OriginSearchDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private OriginManager originManager;
  @Mock
  private OriginResponseConverter originResponseConverter;
  @InjectMocks
  private OriginSearchDelegate delegate;

  @BeforeEach
  public void setUp() {
    lenient().when(originManager.fetchOrigins()).thenReturn(createEntities());
    lenient().when(originResponseConverter.apply(notNull())).thenReturn(Origin.builder().build());
    lenient().when(securityContext.hasReadPermission(isA(OriginEntity.class))).thenReturn(true);
  }

  @Test
  public void testSearchOriginWithoutGeneralViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoOrigin);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new SearchOriginRequest()));
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
