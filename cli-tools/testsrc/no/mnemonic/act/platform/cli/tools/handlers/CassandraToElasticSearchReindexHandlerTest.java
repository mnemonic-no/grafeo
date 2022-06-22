package no.mnemonic.act.platform.cli.tools.handlers;

import no.mnemonic.act.platform.cli.tools.converters.FactEntityToDocumentConverter;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactRefreshLogEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.FactSearchManager.TargetIndex.Daily;
import static no.mnemonic.act.platform.dao.elastic.FactSearchManager.TargetIndex.TimeGlobal;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CassandraToElasticSearchReindexHandlerTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private FactEntityToDocumentConverter factConverter;
  @Mock
  private CassandraFactProcessor factProcessor;
  @InjectMocks
  private CassandraToElasticSearchReindexHandler handler;

  @Test
  public void testReindexInvokesFactProcessor() {
    Instant start = Instant.parse("2021-01-01T12:00:00.000Z");
    Instant stop = Instant.parse("2021-01-01T17:30:00.000Z");

    assertDoesNotThrow(() -> handler.reindex(start, stop, false));
    verify(factProcessor).process(notNull(), eq(start), eq(stop), eq(false));
  }

  @Test
  public void testReindexTimeGlobalFact() {
    FactEntity entity = new FactEntity().addFlag(FactEntity.Flag.TimeGlobalIndex);
    FactDocument document = new FactDocument();
    when(factConverter.apply(notNull(), any())).thenReturn(document);

    assertDoesNotThrow(() -> handler.reindexSingleFact(entity));
    verify(factConverter).apply(entity, null);
    verify(factSearchManager).indexFact(document, TimeGlobal);
    verifyNoInteractions(factManager);
  }

  @Test
  public void testReindexDailyFactSingleRefreshLogEntry() {
    FactEntity fact = new FactEntity().setId(UUID.randomUUID());
    FactRefreshLogEntity logEntry = new FactRefreshLogEntity();
    FactDocument document = new FactDocument();
    when(factManager.fetchFactRefreshLog(notNull())).thenReturn(ListUtils.list(logEntry));
    when(factConverter.apply(notNull(), any())).thenReturn(document);

    assertDoesNotThrow(() -> handler.reindexSingleFact(fact));
    verify(factManager).fetchFactRefreshLog(fact.getId());
    verify(factConverter).apply(fact, null);
    verify(factSearchManager).indexFact(document, Daily);
  }

  @Test
  public void testReindexDailyFactMultipleRefreshLogEntries() {
    FactEntity fact = new FactEntity().setId(UUID.randomUUID());
    FactRefreshLogEntity logEntry1 = new FactRefreshLogEntity();
    FactRefreshLogEntity logEntry2 = new FactRefreshLogEntity();
    FactRefreshLogEntity logEntry3 = new FactRefreshLogEntity();
    FactDocument document = new FactDocument();
    when(factManager.fetchFactRefreshLog(notNull())).thenReturn(ListUtils.list(logEntry1, logEntry2, logEntry3));
    when(factConverter.apply(notNull(), any())).thenReturn(document);

    assertDoesNotThrow(() -> handler.reindexSingleFact(fact));
    verify(factManager).fetchFactRefreshLog(fact.getId());
    verify(factConverter).apply(fact, logEntry1);
    verify(factConverter).apply(fact, logEntry2);
    verify(factConverter).apply(fact, null);
    verify(factSearchManager, times(3)).indexFact(document, Daily);
  }
}
