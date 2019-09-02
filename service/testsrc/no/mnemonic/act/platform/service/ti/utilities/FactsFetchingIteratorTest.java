package no.mnemonic.act.platform.service.ti.utilities;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactsFetchingIteratorTest {

  @Mock
  private FactManager factManager;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test(expected = RuntimeException.class)
  public void testInitializeWithoutFactManager() {
    new FactsFetchingIterator(null, Collections.emptyIterator());
  }

  @Test(expected = RuntimeException.class)
  public void testInitializeWithoutInputIterator() {
    new FactsFetchingIterator(factManager, null);
  }

  @Test
  public void testIteratorNoBatch() {
    when(factManager.getFacts(any())).thenReturn(Collections.emptyIterator());
    FactsFetchingIterator iterator = new FactsFetchingIterator(factManager, Collections.emptyIterator());

    assertTrue(ListUtils.list(iterator).isEmpty());
    verify(factManager).getFacts(argThat(List::isEmpty));
  }

  @Test
  public void testIteratorSingleElement() {
    when(factManager.getFacts(any())).thenReturn(Collections.singletonList(new FactEntity()).iterator());
    FactsFetchingIterator iterator = new FactsFetchingIterator(factManager, Collections.singletonList(UUID.randomUUID()).iterator());

    assertEquals(1, ListUtils.list(iterator).size());
    verify(factManager).getFacts(argThat(list -> list.size() == 1));
  }

  @Test
  public void testIteratorSingleBatch() {
    int size = 999;

    when(factManager.getFacts(any())).thenReturn(generateList(FactEntity::new, size).iterator());
    FactsFetchingIterator iterator = new FactsFetchingIterator(factManager, generateList(UUID::randomUUID, size).iterator());

    assertEquals(size, ListUtils.list(iterator).size());
    verify(factManager).getFacts(argThat(list -> list.size() == size));
  }

  @Test
  public void testIteratorMultipleBatches() {
    int size = 1001;

    when(factManager.getFacts(any()))
            .thenReturn(generateList(FactEntity::new, 1000).iterator(), generateList(FactEntity::new, 1).iterator());
    FactsFetchingIterator iterator = new FactsFetchingIterator(factManager, generateList(UUID::randomUUID, size).iterator());

    assertEquals(size, ListUtils.list(iterator).size());
    // 2 batches + 1 empty call
    verify(factManager, times(3)).getFacts(notNull());
  }

  private <T> List<T> generateList(Supplier<T> generator, int size) {
    List<T> id = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      id.add(generator.get());
    }

    return id;
  }
}
