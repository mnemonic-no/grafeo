package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ObjectTypeSearchDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectTypesWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewTypes);
    ObjectTypeSearchDelegate.create().handle(new SearchObjectTypeRequest());
  }

  @Test
  public void testFetchObjectTypes() throws Exception {
    List<ObjectTypeEntity> entities = ListUtils.list(new ObjectTypeEntity(), new ObjectTypeEntity(), new ObjectTypeEntity());
    when(getObjectManager().fetchObjectTypes()).thenReturn(entities);

    ResultSet<ObjectType> result = ObjectTypeSearchDelegate.create().handle(new SearchObjectTypeRequest());

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), ListUtils.list(result.iterator()).size());
    verify(getObjectTypeConverter(), times(entities.size())).apply(argThat(entities::contains));
  }

}
