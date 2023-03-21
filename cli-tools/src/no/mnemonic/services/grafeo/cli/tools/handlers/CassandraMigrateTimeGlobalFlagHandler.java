package no.mnemonic.services.grafeo.cli.tools.handlers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

@Singleton
public class CassandraMigrateTimeGlobalFlagHandler {

  private static final Logger LOGGER = Logging.getLogger(CassandraMigrateTimeGlobalFlagHandler.class);
  // This constant must be the same as the one defined in FactTypeRequestResolver!
  static final UUID RETRACTION_FACT_TYPE_ID = UUID.nameUUIDFromBytes("SystemRetractionFactType".getBytes());

  @Dependency
  private final FactManager factManager;
  @Dependency
  private final ObjectManager objectManager;

  private final CassandraFactProcessor factProcessor;
  // For each resolved Object cache whether the Object is time global to reduce the number of calls towards Cassandra.
  private final LoadingCache<UUID, Boolean> timeGlobalObjectCache;

  @Inject
  public CassandraMigrateTimeGlobalFlagHandler(
          FactManager factManager,
          ObjectManager objectManager,
          CassandraFactProcessor factProcessor
  ) {
    this.factManager = factManager;
    this.objectManager = objectManager;
    this.factProcessor = factProcessor;
    this.timeGlobalObjectCache = createTimeGlobalObjectCache();
  }

  /**
   * Migrate the TimeGlobal flag for Facts in Cassandra.
   * <p>
   * Fetches all Facts created between startTimestamp and endTimestamp from Cassandra and sets the TimeGlobal flag if applicable.
   *
   * @param startTimestamp Timestamp to start migrating
   * @param endTimestamp   Timestamp to stop migrating
   */
  public void migrate(Instant startTimestamp, Instant endTimestamp) {
    LOGGER.info("Migrate TimeGlobal flag for Facts between %s and %s.", startTimestamp, endTimestamp);

    LongAdder processedFacts = new LongAdder();
    factProcessor.process(fact -> {
      migrateSingleFact(fact);
      processedFacts.increment();
    }, startTimestamp, endTimestamp, false);

    LOGGER.info("Finished migrating, processed %d Facts in total.", processedFacts.longValue());
  }

  void migrateSingleFact(FactEntity fact) {
    // Flag is already set, nothing to do.
    if (fact.isSet(FactEntity.Flag.TimeGlobalIndex)) return;

    if (isTimeGlobal(fact)) {
      // The Fact is time global but that hasn't been stored in Cassandra yet.
      factManager.saveFact(fact.addFlag(FactEntity.Flag.TimeGlobalIndex));
    }
  }

  private boolean isTimeGlobal(FactEntity fact) {
    return isTimeGlobalRetractionFact(fact) || isTimeGlobalMetaFact(fact) || isTimeGlobalObjectFact(fact);
  }

  private boolean isTimeGlobalRetractionFact(FactEntity fact) {
    return Objects.equals(fact.getTypeID(), RETRACTION_FACT_TYPE_ID);
  }

  private boolean isTimeGlobalMetaFact(FactEntity fact) {
    FactEntity referencedFact = factManager.getFact(fact.getInReferenceToID());
    if (referencedFact == null) return false;

    // Check whether the referenced Fact is time global. Then the meta Fact is time global as well. Note that the
    // referenced Fact must have been created before the meta Fact and that Facts will be processed sorted by creation
    // timestamp (oldest first). Because of that, the time global flag has already been set on the referenced Fact if
    // it's indeed time global.
    return referencedFact.isSet(FactEntity.Flag.TimeGlobalIndex);
  }

  private boolean isTimeGlobalObjectFact(FactEntity fact) {
    if (CollectionUtils.isEmpty(fact.getBindings())) return false;

    // A Fact is time global if all bound Objects are configured to be time global.
    return fact.getBindings()
            .stream()
            .allMatch(binding -> isTimeGlobalObject(binding.getObjectID()));
  }

  private boolean isTimeGlobalObject(UUID objectID) {
    return timeGlobalObjectCache.getUnchecked(objectID);
  }

  private LoadingCache<UUID, Boolean> createTimeGlobalObjectCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(15))
            .maximumSize(1_000_000)
            .build(new CacheLoader<UUID, Boolean>() {
              @Override
              public Boolean load(UUID key) {
                ObjectEntity object = objectManager.getObject(key);
                return objectManager.getObjectType(object.getTypeID()).isSet(ObjectTypeEntity.Flag.TimeGlobalIndex);
              }
            });
  }
}
