package no.mnemonic.act.platform.cli.tools.handlers;

import no.mnemonic.act.platform.cli.tools.converters.FactEntityToDocumentConverter;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CassandraToElasticSearchReindexHandlerTest {

  private static final Instant DAY1_1 = Instant.parse("2021-01-01T12:00:00.000Z");
  private static final Instant DAY1_2 = Instant.parse("2021-01-01T17:30:00.000Z");
  private static final Instant DAY2 = Instant.parse("2021-01-02T12:00:00.000Z");
  private static final Instant DAY3_1 = Instant.parse("2021-01-03T12:00:00.000Z");
  private static final Instant DAY3_2 = Instant.parse("2021-01-03T17:30:00.000Z");

  @Mock
  private FactManager factManager;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private FactEntityToDocumentConverter factConverter;
  @InjectMocks
  private CassandraToElasticSearchReindexHandler handler;

  @Test
  public void testReindexEndBeforeStart() {
    assertDoesNotThrow(() -> handler.reindex(DAY2, DAY1_1, false));
    verifyNoInteractions(factManager, factSearchManager, factConverter);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testReindexSingleBucket(boolean reverse) {
    FactEntity entity = new FactEntity();
    FactDocument document = new FactDocument();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenReturn(ListUtils.list(entity).iterator());
    when(factConverter.apply(notNull())).thenReturn(document);

    assertDoesNotThrow(() -> handler.reindex(DAY1_1, DAY2, reverse));
    verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    verify(factConverter).apply(entity);
    verify(factSearchManager).indexFact(document);
    verifyNoMoreInteractions(factManager, factSearchManager, factConverter);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testReindexSingleBucketUnevenBucketSize(boolean reverse) {
    FactEntity entity = new FactEntity();
    FactDocument document = new FactDocument();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenReturn(ListUtils.list(entity).iterator());
    when(factConverter.apply(notNull())).thenReturn(document);

    assertDoesNotThrow(() -> handler.reindex(DAY1_1, DAY1_2, reverse));
    verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY1_2.toEpochMilli());
    verify(factConverter).apply(entity);
    verify(factSearchManager).indexFact(document);
    verifyNoMoreInteractions(factManager, factSearchManager, factConverter);
  }

  @Test
  public void testReindexMultipleBuckets() {
    FactEntity entity = new FactEntity();
    FactDocument document = new FactDocument();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());
    when(factConverter.apply(notNull())).thenAnswer(i -> document);

    assertDoesNotThrow(() -> handler.reindex(DAY1_1, DAY3_1, false));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    verify(factConverter, times(2)).apply(entity);
    verify(factSearchManager, times(2)).indexFact(document);
    verifyNoMoreInteractions(factManager, factSearchManager, factConverter);
  }

  @Test
  public void testReindexMultipleBucketsReversed() {
    FactEntity entity = new FactEntity();
    FactDocument document = new FactDocument();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());
    when(factConverter.apply(notNull())).thenAnswer(i -> document);

    assertDoesNotThrow(() -> handler.reindex(DAY1_1, DAY3_1, true));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    verify(factConverter, times(2)).apply(entity);
    verify(factSearchManager, times(2)).indexFact(document);
    verifyNoMoreInteractions(factManager, factSearchManager, factConverter);
  }

  @Test
  public void testReindexMultipleBucketsUnevenBucketSize() {
    FactEntity entity = new FactEntity();
    FactDocument document = new FactDocument();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());
    when(factConverter.apply(notNull())).thenAnswer(i -> document);

    assertDoesNotThrow(() -> handler.reindex(DAY1_1, DAY3_2, false));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_1.toEpochMilli(), DAY2.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY3_1.toEpochMilli(), DAY3_2.toEpochMilli());
    verify(factConverter, times(3)).apply(entity);
    verify(factSearchManager, times(3)).indexFact(document);
    verifyNoMoreInteractions(factManager, factSearchManager, factConverter);
  }

  @Test
  public void testReindexMultipleBucketsUnevenBucketSizeReversed() {
    FactEntity entity = new FactEntity();
    FactDocument document = new FactDocument();
    when(factManager.getFactsWithin(anyLong(), anyLong())).thenAnswer(i -> ListUtils.list(entity).iterator());
    when(factConverter.apply(notNull())).thenAnswer(i -> document);

    assertDoesNotThrow(() -> handler.reindex(DAY1_2, DAY3_1, true));
    InOrder bucketsOrder = inOrder(factManager);
    bucketsOrder.verify(factManager).getFactsWithin(DAY2.toEpochMilli(), DAY3_1.toEpochMilli());
    bucketsOrder.verify(factManager).getFactsWithin(DAY1_2.toEpochMilli(), DAY2.toEpochMilli());
    verify(factConverter, times(2)).apply(entity);
    verify(factSearchManager, times(2)).indexFact(document);
    verifyNoMoreInteractions(factManager, factSearchManager, factConverter);
  }
}
