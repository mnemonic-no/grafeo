package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.seb.model.v1.OriginInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginInfoDaoResolverTest {

  @Mock
  private OriginManager originManager;

  private OriginInfoDaoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new OriginInfoDaoResolver(originManager);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoOriginFound() {
    UUID id = UUID.randomUUID();

    OriginInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());
    assertEquals("N/A", seb.getName());

    verify(originManager).getOrigin(id);
  }

  @Test
  public void testResolveOriginFound() {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID())
            .setName("name");
    when(originManager.getOrigin(isA(UUID.class))).thenReturn(entity);

    OriginInfoSEB seb = resolver.apply(entity.getId());
    assertNotNull(seb);
    assertEquals(entity.getId(), seb.getId());
    assertEquals(entity.getName(), seb.getName());

    verify(originManager).getOrigin(entity.getId());
  }
}
