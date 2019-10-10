package no.mnemonic.act.platform.dao.facade.utilities;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class BatchingIteratorTest {

  @Mock
  private Function<List<UUID>, Iterator<Object>> nextBatch;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test(expected = RuntimeException.class)
  public void testInitializeWithoutNextBatch() {
    new BatchingIterator<>(generateIterator(UUID::randomUUID, 3), null);
  }

  @Test
  public void testInitializeWithoutIterator() {
    Iterator<Object> iterator = new BatchingIterator<>(null, nextBatch);
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testIteratorSingleElement() {
    when(nextBatch.apply(anyList())).thenReturn(generateIterator(Object::new, 1));
    Iterator<Object> iterator = new BatchingIterator<>(generateIterator(UUID::randomUUID, 1), nextBatch);

    assertEquals(1, ListUtils.list(iterator).size());
    verify(nextBatch).apply(argThat(list -> list.size() == 1));
  }

  @Test
  public void testIteratorSingleBatch() {
    int size = 999;

    when(nextBatch.apply(anyList())).thenReturn(generateIterator(Object::new, size));
    Iterator<Object> iterator = new BatchingIterator<>(generateIterator(UUID::randomUUID, size), nextBatch);

    assertEquals(size, ListUtils.list(iterator).size());
    verify(nextBatch).apply(argThat(list -> list.size() == size));
  }

  @Test
  public void testIteratorMultipleBatches() {
    int size = 1001;

    when(nextBatch.apply(anyList()))
            .thenReturn(generateIterator(Object::new, 1000))
            .thenReturn(generateIterator(Object::new, 1));
    Iterator<Object> iterator = new BatchingIterator<>(generateIterator(UUID::randomUUID, size), nextBatch);

    assertEquals(size, ListUtils.list(iterator).size());
    verify(nextBatch, times(2)).apply(notNull());
  }

  private <T> Iterator<T> generateIterator(Supplier<T> generator, int size) {
    List<T> id = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      id.add(generator.get());
    }

    return id.iterator();
  }
}
