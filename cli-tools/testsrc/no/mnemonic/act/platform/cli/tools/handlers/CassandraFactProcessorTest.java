package no.mnemonic.act.platform.cli.tools.handlers;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CassandraFactProcessorTest {

  private static final Instant DAY1_1 = Instant.parse("2021-01-01T12:00:00.000Z");
  private static final Instant DAY1_2 = Instant.parse("2021-01-01T17:30:00.000Z");
  private static final Instant DAY2 = Instant.parse("2021-01-02T12:00:00.000Z");
  private static final Instant DAY3_1 = Instant.parse("2021-01-03T12:00:00.000Z");
  private static final Instant DAY3_2 = Instant.parse("2021-01-03T17:30:00.000Z");

  @Mock
  private FactManager factManager;
  @Mock
  private Consumer<FactEntity> operation;
  @InjectMocks
  private CassandraFactProcessor processor;

  @Test
  public void testProcessEndBeforeStart() {
    assertDoesNotThrow(() -> processor.process(operation, DAY2, DAY1_1, false));
    verifyNoInteractions(factManager, operation);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testProcessSingleBucket(boolean reverse) {
    FactEntity entity = new FactEntity();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenReturn(ListUtils.list(entity).iterator());

    assertDoesNotThrow(() -> processor.process(operation, DAY1_1, DAY2, reverse));
    verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    verify(operation).accept(entity);
    verifyNoMoreInteractions(factManager);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testProcessSingleBucketUnevenBucketSize(boolean reverse) {
    FactEntity entity = new FactEntity();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenReturn(ListUtils.list(entity).iterator());

    assertDoesNotThrow(() -> processor.process(operation, DAY1_1, DAY1_2, reverse));
    verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY1_2.toEpochMilli());
    verify(operation).accept(entity);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testProcessMultipleBuckets() {
    FactEntity entity = new FactEntity();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());

    assertDoesNotThrow(() -> processor.process(operation, DAY1_1, DAY3_1, false));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    verify(operation, times(2)).accept(entity);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testProcessMultipleBucketsReversed() {
    FactEntity entity = new FactEntity();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());

    assertDoesNotThrow(() -> processor.process(operation, DAY1_1, DAY3_1, true));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    verify(operation, times(2)).accept(entity);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testProcessMultipleBucketsUnevenBucketSize() {
    FactEntity entity = new FactEntity();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());

    assertDoesNotThrow(() -> processor.process(operation, DAY1_1, DAY3_2, false));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY3_1.toEpochMilli(), DAY3_2.toEpochMilli());
    verify(operation, times(3)).accept(entity);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testProcessMultipleBucketsUnevenBucketSizeReversed() {
    FactEntity entity = new FactEntity();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());

    assertDoesNotThrow(() -> processor.process(operation, DAY1_2, DAY3_1, true));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_2.toEpochMilli(), DAY2.toEpochMilli());
    verify(operation, times(2)).accept(entity);
    verifyNoMoreInteractions(factManager);
  }
}
