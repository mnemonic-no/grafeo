package no.mnemonic.act.platform.dao.facade.resolvers;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.facade.converters.FactRecordConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GuavaBackedFactResolverTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactRecordConverter factRecordConverter;

  private CachedFactResolver factResolver;

  @Before
  public void setUp() {
    initMocks(this);
    factResolver = new GuavaBackedFactResolver(factManager, factRecordConverter, new GuavaBackedFactResolver.CacheConfiguration());
  }

  @Test
  public void testGetFactByIdInvalidInput() {
    assertNull(factResolver.getFact(null));
    verifyNoInteractions(factManager);
  }

  @Test
  public void testGetFactByIdNotFound() {
    UUID id = UUID.randomUUID();
    assertNull(factResolver.getFact(id));
    verify(factManager).getFact(id);
  }

  @Test
  public void testGetFactByIdFound() {
    UUID id = UUID.randomUUID();
    when(factManager.getFact(any())).thenReturn(new FactEntity());
    when(factRecordConverter.fromEntity(any())).thenReturn(new FactRecord().setId(id));

    assertNotNull(factResolver.getFact(id));
    verify(factManager).getFact(id);
    verify(factRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testGetFactByIdFoundCached() {
    UUID id = UUID.randomUUID();
    when(factManager.getFact(any())).thenReturn(new FactEntity());
    when(factRecordConverter.fromEntity(any())).then(i -> new FactRecord().setId(id));

    assertSame(factResolver.getFact(id), factResolver.getFact(id));
    verify(factManager).getFact(id);
    verify(factRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testEvictInvalidInput() {
    factResolver.evict(null);
    factResolver.evict(new FactRecord());
    factResolver.evict(new FactRecord().setId(UUID.randomUUID()));
  }

  @Test
  public void testGetFactByIdAfterEvict() {
    UUID id = UUID.randomUUID();
    when(factManager.getFact(any())).thenReturn(new FactEntity());
    when(factRecordConverter.fromEntity(any())).then(i -> new FactRecord().setId(id));

    FactRecord fact1 = factResolver.getFact(id);
    factResolver.evict(fact1);
    FactRecord fact2 = factResolver.getFact(id);

    assertNotSame(fact1, fact2);
    verify(factManager, times(2)).getFact(id);
    verify(factRecordConverter, times(2)).fromEntity(notNull());
  }
}
