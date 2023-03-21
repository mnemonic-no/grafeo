package no.mnemonic.services.grafeo.cli.tools.handlers;

import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.services.grafeo.cli.tools.converters.FactEntityToDocumentConverter;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactRefreshLogEntity;
import no.mnemonic.services.grafeo.dao.elastic.FactSearchManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.Daily;
import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.TimeGlobal;

@Singleton
public class CassandraToElasticSearchReindexHandler {

  private static final Logger LOGGER = Logging.getLogger(CassandraToElasticSearchReindexHandler.class);

  @Dependency
  private final FactManager factManager;
  @Dependency
  private final FactSearchManager factSearchManager;

  private final CassandraFactProcessor factProcessor;
  private final FactEntityToDocumentConverter factConverter;

  @Inject
  public CassandraToElasticSearchReindexHandler(
          FactManager factManager,
          FactSearchManager factSearchManager,
          CassandraFactProcessor factProcessor,
          FactEntityToDocumentConverter factConverter
  ) {
    this.factManager = factManager;
    this.factSearchManager = factSearchManager;
    this.factProcessor = factProcessor;
    this.factConverter = factConverter;
  }

  /**
   * Reindex Facts from Cassandra into ElasticSearch.
   * <p>
   * Fetches all Facts created between startTimestamp and endTimestamp from Cassandra and indexes them into ElasticSearch.
   *
   * @param startTimestamp Timestamp to start reindexing
   * @param endTimestamp   Timestamp to stop reindexing
   * @param reverse        If true reverse the reindexing order
   */
  public void reindex(Instant startTimestamp, Instant endTimestamp, boolean reverse) {
    LOGGER.info("Reindex Facts between %s and %s.", startTimestamp, endTimestamp);

    LongAdder processedFacts = new LongAdder();
    factProcessor.process(fact -> {
      reindexSingleFact(fact);
      processedFacts.increment();
    }, startTimestamp, endTimestamp, reverse);

    LOGGER.info("Finished reindexing Facts, processed %d Facts in total.", processedFacts.longValue());
  }

  /**
   * Reindex Facts from Cassandra into ElasticSearch.
   * <p>
   * Fetches all Facts by ID from Cassandra and indexes them into ElasticSearch.
   *
   * @param ids IDs of Facts to reindex
   */
  public void reindex(Set<UUID> ids) {
    LOGGER.info("Reindex Facts with IDs = %s.", ids);

    LongAdder processedFacts = new LongAdder();
    set(ids).forEach(id -> {
      FactEntity fact = factManager.getFact(id);
      if (fact == null) {
        LOGGER.warning("Fact with id = %s could not be retrieved from Cassandra. Skipping it!", id);
        return;
      }

      reindexSingleFact(fact);
      processedFacts.increment();
    });

    LOGGER.info("Finished reindexing Facts, processed %d Facts in total.", processedFacts.longValue());
  }

  void reindexSingleFact(FactEntity fact) {
    if (fact.isSet(FactEntity.Flag.TimeGlobalIndex)) {
      factSearchManager.indexFact(factConverter.apply(fact, null), TimeGlobal);
    } else {
      List<FactRefreshLogEntity> refreshLog = factManager.fetchFactRefreshLog(fact.getId());
      for (int i = 0; i < refreshLog.size(); i++) {
        FactRefreshLogEntity logEntry = refreshLog.get(i);
        if (i + 1 == refreshLog.size()) {
          // For the last entry pass NULL to the converter because the document needs to contain all ACL entries.
          // A user might have been granted access to a Fact after it was refreshed.
          factSearchManager.indexFact(factConverter.apply(fact, null), Daily);
        } else {
          factSearchManager.indexFact(factConverter.apply(fact, logEntry), Daily);
        }
      }
    }
  }
}
