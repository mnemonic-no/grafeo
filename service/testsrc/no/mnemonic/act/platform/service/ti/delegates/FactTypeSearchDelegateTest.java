package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactTypeResponseConverter;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
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
  private TiSecurityContext securityContext;

  private FactTypeSearchDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactTypeSearchDelegate(securityContext, factManager, factTypeResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactTypesWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewThreatIntelType);
    delegate.handle(new SearchFactTypeRequest());
  }

  @Test
  public void testFetchFactTypes() throws Exception {
    List<FactTypeEntity> entities = ListUtils.list(new FactTypeEntity(), new FactTypeEntity(), new FactTypeEntity());
    when(factManager.fetchFactTypes()).thenReturn(entities);

    ResultSet<FactType> result = delegate.handle(new SearchFactTypeRequest());

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), ListUtils.list(result.iterator()).size());
    verify(factTypeResponseConverter, times(entities.size())).apply(argThat(entities::contains));
  }
}
