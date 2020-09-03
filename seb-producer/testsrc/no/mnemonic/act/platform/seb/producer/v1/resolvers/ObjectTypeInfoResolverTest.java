package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.seb.model.v1.ObjectTypeInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectTypeInfoResolverTest {

  @Mock
  private ObjectManager objectManager;

  private ObjectTypeInfoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new ObjectTypeInfoResolver(objectManager);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoTypeFound() {
    UUID id = UUID.randomUUID();
    assertNull(resolver.apply(id));
    verify(objectManager).getObjectType(id);
  }

  @Test
  public void testResolveTypeFound() {
    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("name");
    when(objectManager.getObjectType(isA(UUID.class))).thenReturn(entity);

    ObjectTypeInfoSEB seb = resolver.apply(entity.getId());
    assertNotNull(seb);
    assertEquals(entity.getId(), seb.getId());
    assertEquals(entity.getName(), seb.getName());

    verify(objectManager).getObjectType(entity.getId());
  }
}
