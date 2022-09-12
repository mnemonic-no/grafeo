package no.mnemonic.act.platform.dao.facade.resolvers;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.facade.converters.FactRecordConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MapBackedFactResolverTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactRecordConverter factRecordConverter;

  private CachedFactResolver factResolver;

  @Before
  public void setUp() {
    initMocks(this);
    factResolver = new MapBackedFactResolver(factManager, factRecordConverter, new HashMap<>(), new HashMap<>());
  }

  @Test
  public void testGetFactByIdInvalidInput() {
    assertNull(factResolver.getFact((UUID) null));
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
    when(factManager.getFact(isA(UUID.class))).thenReturn(new FactEntity());
    when(factRecordConverter.fromEntity(any())).thenReturn(new FactRecord().setId(id));

    assertNotNull(factResolver.getFact(id));
    verify(factManager).getFact(id);
    verify(factRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testGetFactByIdFoundCached() {
    UUID id = UUID.randomUUID();
    when(factManager.getFact(isA(UUID.class))).thenReturn(new FactEntity());
    when(factRecordConverter.fromEntity(any())).then(i -> new FactRecord().setId(id));

    assertSame(factResolver.getFact(id), factResolver.getFact(id));
    verify(factManager).getFact(id);
    verify(factRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testGetFactByHashInvalidInput() {
    assertNull(factResolver.getFact((String) null));
    assertNull(factResolver.getFact(""));
    assertNull(factResolver.getFact(" "));
    verifyNoInteractions(factManager);
  }

  @Test
  public void testGetFactByHashNotFound() {
    String hash = "abc789";
    assertNull(factResolver.getFact(hash));
    verify(factManager).getFact(hash);
    verify(factManager, never()).getFact(isA(UUID.class));
  }

  @Test
  public void testGetFactByHashFound() {
    String hash = "abc789";
    UUID id = UUID.randomUUID();
    when(factManager.getFact(isA(String.class))).thenReturn(new FactEntity().setId(id));
    when(factManager.getFact(isA(UUID.class))).thenReturn(new FactEntity().setId(id));
    when(factRecordConverter.fromEntity(any())).thenReturn(new FactRecord());

    assertNotNull(factResolver.getFact(hash));
    verify(factManager).getFact(hash);
    verify(factManager).getFact(id);
    verify(factRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testGetFactByHashFoundCached() {
    String hash = "abc789";
    UUID id = UUID.randomUUID();
    when(factManager.getFact(isA(String.class))).thenReturn(new FactEntity().setId(id));
    when(factManager.getFact(isA(UUID.class))).thenReturn(new FactEntity().setId(id));
    when(factRecordConverter.fromEntity(any())).then(i -> new FactRecord());

    assertSame(factResolver.getFact(hash), factResolver.getFact(hash));
    verify(factManager).getFact(hash);
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
    when(factManager.getFact(isA(UUID.class))).thenReturn(new FactEntity());
    when(factRecordConverter.fromEntity(any())).then(i -> new FactRecord().setId(id));

    FactRecord fact1 = factResolver.getFact(id);
    factResolver.evict(fact1);
    FactRecord fact2 = factResolver.getFact(id);

    assertNotSame(fact1, fact2);
    verify(factManager, times(2)).getFact(id);
    verify(factRecordConverter, times(2)).fromEntity(notNull());
  }

  @Test
  public void testGetFactByHashAfterEvict() {
    String hash = "abc789";
    UUID id = UUID.randomUUID();
    when(factManager.getFact(isA(String.class))).thenReturn(new FactEntity().setId(id));
    when(factManager.getFact(isA(UUID.class))).thenReturn(new FactEntity().setId(id));
    when(factRecordConverter.fromEntity(any())).then(i -> new FactRecord().setId(id));

    FactRecord fact1 = factResolver.getFact(hash);
    factResolver.evict(fact1);
    FactRecord fact2 = factResolver.getFact(hash);

    assertNotSame(fact1, fact2);
    verify(factManager).getFact(hash);
    verify(factManager, times(2)).getFact(id);
    verify(factRecordConverter, times(2)).fromEntity(notNull());
  }
}
