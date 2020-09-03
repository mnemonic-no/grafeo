package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.seb.model.v1.OriginInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginInfoResolverTest {

  @Mock
  private OriginManager originManager;

  private OriginInfoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new OriginInfoResolver(originManager);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoOriginFound() {
    UUID id = UUID.randomUUID();
    assertNull(resolver.apply(id));
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
