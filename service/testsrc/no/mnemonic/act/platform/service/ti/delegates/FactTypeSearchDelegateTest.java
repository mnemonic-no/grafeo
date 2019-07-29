package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.SearchFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class FactTypeSearchDelegateTest extends AbstractDelegateTest {

  private FactTypeSearchDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactTypeSearchDelegate(getSecurityContext(), getFactManager(), getFactTypeConverter());
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactTypesWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewTypes);
    delegate.handle(new SearchFactTypeRequest());
  }

  @Test
  public void testFetchFactTypes() throws Exception {
    List<FactTypeEntity> entities = ListUtils.list(new FactTypeEntity(), new FactTypeEntity(), new FactTypeEntity());
    when(getFactManager().fetchFactTypes()).thenReturn(entities);

    ResultSet<FactType> result = delegate.handle(new SearchFactTypeRequest());

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), ListUtils.list(result.iterator()).size());
    verify(getFactTypeConverter(), times(entities.size())).apply(argThat(entities::contains));
  }
}
