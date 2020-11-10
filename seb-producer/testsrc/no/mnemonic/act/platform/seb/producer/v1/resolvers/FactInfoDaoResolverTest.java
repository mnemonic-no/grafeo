package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.seb.model.v1.FactInfoSEB;
import no.mnemonic.act.platform.seb.model.v1.FactTypeInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactInfoDaoResolverTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactTypeInfoDaoResolver typeResolver;

  private FactInfoDaoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new FactInfoDaoResolver(factManager, typeResolver);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoFactFound() {
    UUID id = UUID.randomUUID();

    FactInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());

    verify(factManager).getFact(id);
  }

  @Test
  public void testResolveFactFound() {
    FactEntity entity = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");
    when(factManager.getFact(any())).thenReturn(entity);
    when(typeResolver.apply(any())).thenReturn(FactTypeInfoSEB.builder().build());

    FactInfoSEB seb = resolver.apply(entity.getId());
    assertNotNull(seb);
    assertEquals(entity.getId(), seb.getId());
    assertNotNull(seb.getType());
    assertEquals(entity.getValue(), seb.getValue());

    verify(factManager).getFact(entity.getId());
    verify(typeResolver).apply(entity.getTypeID());
  }
}
