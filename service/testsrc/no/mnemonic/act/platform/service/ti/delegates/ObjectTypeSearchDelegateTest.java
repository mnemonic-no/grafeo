package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeConverter;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectTypeSearchDelegateTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private ObjectTypeConverter objectTypeConverter;
  @Mock
  private TiSecurityContext securityContext;

  private ObjectTypeSearchDelegate delegate;


  @Before
  public void setup() {
    initMocks(this);
    delegate = new ObjectTypeSearchDelegate(securityContext, objectManager, objectTypeConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectTypesWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewTypes);
    delegate.handle(new SearchObjectTypeRequest());
  }

  @Test
  public void testFetchObjectTypes() throws Exception {
    List<ObjectTypeEntity> entities = ListUtils.list(new ObjectTypeEntity(), new ObjectTypeEntity(), new ObjectTypeEntity());
    when(objectManager.fetchObjectTypes()).thenReturn(entities);

    ResultSet<ObjectType> result = delegate.handle(new SearchObjectTypeRequest());

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), ListUtils.list(result.iterator()).size());
    verify(objectTypeConverter, times(entities.size())).apply(argThat(entities::contains));
  }
}
