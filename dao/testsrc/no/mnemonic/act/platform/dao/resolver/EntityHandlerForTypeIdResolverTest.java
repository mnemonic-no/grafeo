package no.mnemonic.act.platform.dao.resolver;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.handlers.EntityHandler;
import no.mnemonic.act.platform.dao.handlers.EntityHandlerFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class EntityHandlerForTypeIdResolverTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactManager factManager;
  @Mock
  private EntityHandlerFactory factory;
  @Mock
  private EntityHandler handler;

  private EntityHandlerForTypeIdResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    when(factory.get(any(), any())).thenReturn(handler);
    resolver = new EntityHandlerForTypeIdResolver(objectManager, factManager, factory);
  }

  @Test
  public void testResolveByObjectType() {
    UUID id = UUID.randomUUID();
    ObjectTypeEntity type = new ObjectTypeEntity()
            .setEntityHandler("handler")
            .setEntityHandlerParameter("parameter");
    when(objectManager.getObjectType(id)).thenReturn(type);

    assertSame(handler, resolver.apply(id));
    verify(objectManager).getObjectType(id);
    verify(factory).get(type.getEntityHandler(), type.getEntityHandlerParameter());
  }

  @Test
  public void testResolveByFactType() {
    UUID id = UUID.randomUUID();
    FactTypeEntity type = new FactTypeEntity()
            .setEntityHandler("handler")
            .setEntityHandlerParameter("parameter");
    when(factManager.getFactType(id)).thenReturn(type);

    assertSame(handler, resolver.apply(id));
    verify(factManager).getFactType(id);
    verify(factory).get(type.getEntityHandler(), type.getEntityHandlerParameter());
  }

  @Test
  public void testResolveTypeNotFound() {
    UUID id = UUID.randomUUID();

    try {
      resolver.apply(id);
      fail();
    } catch (IllegalArgumentException ignored) {
      verify(objectManager).getObjectType(id);
      verify(factManager).getFactType(id);
      verifyZeroInteractions(factory);
    }
  }

}
