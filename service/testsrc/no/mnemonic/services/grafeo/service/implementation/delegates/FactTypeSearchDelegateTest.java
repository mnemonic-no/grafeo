package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactTypeResponseConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeSearchDelegateTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactTypeResponseConverter factTypeResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;

  private FactTypeSearchDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactTypeSearchDelegate(securityContext, factManager, factTypeResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactTypesWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoType);
    delegate.handle(new SearchFactTypeRequest());
  }

  @Test
  public void testFetchFactTypes() throws Exception {
    List<FactTypeEntity> entities = ListUtils.list(new FactTypeEntity(), new FactTypeEntity(), new FactTypeEntity());
    when(factManager.fetchFactTypes()).thenReturn(entities);
    when(factTypeResponseConverter.apply(notNull())).thenReturn(FactType.builder().build());

    ResultSet<FactType> result = delegate.handle(new SearchFactTypeRequest());

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), ListUtils.list(result.iterator()).size());
    verify(factTypeResponseConverter, times(entities.size())).apply(argThat(entities::contains));
  }
}
