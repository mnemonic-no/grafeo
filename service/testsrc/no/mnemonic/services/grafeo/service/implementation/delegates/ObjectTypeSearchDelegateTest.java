package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
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
  private ObjectTypeResponseConverter objectTypeResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;

  private ObjectTypeSearchDelegate delegate;


  @Before
  public void setup() {
    initMocks(this);
    delegate = new ObjectTypeSearchDelegate(securityContext, objectManager, objectTypeResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchObjectTypesWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.viewGrafeoType);
    delegate.handle(new SearchObjectTypeRequest());
  }

  @Test
  public void testFetchObjectTypes() throws Exception {
    List<ObjectTypeEntity> entities = ListUtils.list(new ObjectTypeEntity(), new ObjectTypeEntity(), new ObjectTypeEntity());
    when(objectManager.fetchObjectTypes()).thenReturn(entities);
    when(objectTypeResponseConverter.apply(notNull())).thenReturn(ObjectType.builder().build());

    ResultSet<ObjectType> result = delegate.handle(new SearchObjectTypeRequest());

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), ListUtils.list(result.iterator()).size());
    verify(objectTypeResponseConverter, times(entities.size())).apply(argThat(entities::contains));
  }
}
