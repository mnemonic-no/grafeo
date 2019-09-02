package no.mnemonic.act.platform.dao.cassandra.utilities;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MultiFetchIteratorTest {

  @Mock
  private Function<List<UUID>, Iterator<Object>> nextBatch;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test(expected = RuntimeException.class)
  public void testInitializeWithoutNextBatchFunction() {
    new MultiFetchIterator<>(null, ListUtils.list());
  }

  @Test(expected = RuntimeException.class)
  public void testInitializeWithoutIdList() {
    new MultiFetchIterator<>(nextBatch, null);
  }

  @Test
  public void testIteratorNoBatch() {
    MultiFetchIterator<Object> iterator = new MultiFetchIterator<>(nextBatch, ListUtils.list());
    assertTrue(ListUtils.list(iterator).isEmpty());
    verifyZeroInteractions(nextBatch);
  }

  @Test
  public void testIteratorSingleElement() {
    when(nextBatch.apply(notNull())).thenReturn(Collections.singletonList(new Object()).iterator());
    MultiFetchIterator<Object> iterator = new MultiFetchIterator<>(nextBatch, Collections.singletonList(UUID.randomUUID()));

    assertEquals(1, ListUtils.list(iterator).size());
    verify(nextBatch).apply(argThat(list -> list.size() == 1));
  }

  @Test
  public void testIteratorSingleBatch() {
    int size = 99;

    when(nextBatch.apply(notNull())).thenReturn(generateList(Object::new, size).iterator());
    MultiFetchIterator<Object> iterator = new MultiFetchIterator<>(nextBatch, generateList(UUID::randomUUID, size));

    assertEquals(size, ListUtils.list(iterator).size());
    verify(nextBatch).apply(argThat(list -> list.size() == size));
  }

  @Test
  public void testIteratorMultipleBatches() {
    int size = 101;

    when(nextBatch.apply(notNull()))
            .thenReturn(generateList(Object::new, 100).iterator(), generateList(Object::new, 1).iterator());
    MultiFetchIterator<Object> iterator = new MultiFetchIterator<>(nextBatch, generateList(UUID::randomUUID, size));

    assertEquals(size, ListUtils.list(iterator).size());
    verify(nextBatch, times(2)).apply(notNull());
  }

  private <T> List<T> generateList(Supplier<T> generator, int size) {
    List<T> id = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      id.add(generator.get());
    }

    return id;
  }
}
