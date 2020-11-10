package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.seb.model.v1.FactTypeInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeInfoDaoResolverTest {

  @Mock
  private FactManager factManager;

  private FactTypeInfoDaoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new FactTypeInfoDaoResolver(factManager);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoTypeFound() {
    UUID id = UUID.randomUUID();

    FactTypeInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());
    assertEquals("N/A", seb.getName());

    verify(factManager).getFactType(id);
  }

  @Test
  public void testResolveTypeFound() {
    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("name");
    when(factManager.getFactType(isA(UUID.class))).thenReturn(entity);

    FactTypeInfoSEB seb = resolver.apply(entity.getId());
    assertNotNull(seb);
    assertEquals(entity.getId(), seb.getId());
    assertEquals(entity.getName(), seb.getName());

    verify(factManager).getFactType(entity.getId());
  }
}
