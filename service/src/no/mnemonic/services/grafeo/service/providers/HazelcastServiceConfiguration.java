package no.mnemonic.services.grafeo.service.providers;

import com.google.inject.Inject;
import com.hazelcast.config.*;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;

import javax.inject.Named;
import java.time.Duration;

import static no.mnemonic.services.grafeo.seb.esengine.v1.handlers.FactKafkaToHazelcastHandler.FACT_HAZELCAST_QUEUE_NAME;
import static no.mnemonic.services.grafeo.service.caches.DistributedFactByHashDaoCache.FACT_BY_HASH_MAP_NAME;
import static no.mnemonic.services.grafeo.service.caches.DistributedFactByIdDaoCache.FACT_BY_ID_MAP_NAME;
import static no.mnemonic.services.grafeo.service.caches.DistributedObjectByIdDaoCache.OBJECT_BY_ID_MAP_NAME;
import static no.mnemonic.services.grafeo.service.caches.DistributedObjectByTypeValueDaoCache.OBJECT_BY_TYPE_VALUE_MAP_NAME;
import static no.mnemonic.services.grafeo.service.providers.HazelcastBasedLockProvider.LOCK_MAP_NAME;

/**
 * This class contains the Hazelcast configuration required for the service implementation to function properly
 * (all serializers, queue and map configurations, etc). Use the class to apply the required configuration
 * to an existing Hazelcast {@link Config} object.
 */
public class HazelcastServiceConfiguration {

  private Duration daoCacheFactTimeToLive = Duration.ofMinutes(15);
  private Duration daoCacheObjectTimeToIdle = Duration.ofMinutes(15);
  private Duration daoCacheFactNearCacheTimeToLive = Duration.ofMinutes(5);
  private Duration daoCacheObjectNearCacheTimeToIdle = Duration.ofMinutes(5);
  private int daoCacheFactMaximumCacheSize = 500_000;
  private int daoCacheObjectMaximumCacheSize = 1_000_000;
  private int daoCacheFactNearCacheMaximumCacheSize = 100_000;
  private int daoCacheObjectNearCacheMaximumCacheSize = 200_000;
  private int esEngineFactMaximumQueueSize = 1_000;

  /**
   * Apply the required service configuration to an existing Hazelcast {@link Config} object.
   *
   * @param cfg Existing Hazelcast configuration
   */
  public void apply(Config cfg) {
    if (cfg == null) return;

    applySerializationConfig(cfg);
    applyQueueConfig(cfg);
    applyMapConfig(cfg);
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheFactTimeToLive(@Named("grafeo.dao.cache.fact.ttl") long timeToLive) {
    this.daoCacheFactTimeToLive = Duration.ofMinutes(timeToLive);
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheObjectTimeToIdle(@Named("grafeo.dao.cache.object.ttl") long timeToIdle) {
    this.daoCacheObjectTimeToIdle = Duration.ofMinutes(timeToIdle);
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheFactNearCacheTimeToLive(@Named("grafeo.dao.cache.fact.near.cache.ttl") long timeToLive) {
    this.daoCacheFactNearCacheTimeToLive = Duration.ofMinutes(timeToLive);
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheObjectNearCacheTimeToIdle(@Named("grafeo.dao.cache.object.near.cache.ttl") long timeToIdle) {
    this.daoCacheObjectNearCacheTimeToIdle = Duration.ofMinutes(timeToIdle);
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheFactMaximumCacheSize(@Named("grafeo.dao.cache.fact.size") int maxSize) {
    this.daoCacheFactMaximumCacheSize = maxSize;
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheObjectMaximumCacheSize(@Named("grafeo.dao.cache.object.size") int maxSize) {
    this.daoCacheObjectMaximumCacheSize = maxSize;
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheFactNearCacheMaximumCacheSize(@Named("grafeo.dao.cache.fact.near.cache.size") int maxSize) {
    this.daoCacheFactNearCacheMaximumCacheSize = maxSize;
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setDaoCacheObjectNearCacheMaximumCacheSize(@Named("grafeo.dao.cache.object.near.cache.size") int maxSize) {
    this.daoCacheObjectNearCacheMaximumCacheSize = maxSize;
    return this;
  }

  @Inject(optional = true)
  public HazelcastServiceConfiguration setEsEngineFactMaximumQueueSize(@Named("grafeo.es.engine.fact.queue.size") int maxSize) {
    this.esEngineFactMaximumQueueSize = maxSize;
    return this;
  }

  private void applySerializationConfig(Config cfg) {
    SerializationConfig serializationConfig = cfg.getSerializationConfig();

    // Configure serializers for all classes handled by Hazelcast.
    serializationConfig.addSerializerConfig(createSerializerConfig(FactSEB.class, 46616374)); // ASCII for Fact
    serializationConfig.addSerializerConfig(createSerializerConfig(FactRecord.class, 41435430)); // ASCII for ACT0
    serializationConfig.addSerializerConfig(createSerializerConfig(ObjectRecord.class, 41435431)); // ASCII for ACT1
  }

  private void applyQueueConfig(Config cfg) {
    // Configure the specifics of each Hazelcast queue.
    cfg.getQueueConfig(FACT_HAZELCAST_QUEUE_NAME)
            .setBackupCount(1)
            .setMaxSize(esEngineFactMaximumQueueSize);
  }

  private void applyMapConfig(Config cfg) {
    // Only set backup count as this map is solely used for locking and should never contain any data.
    cfg.getMapConfig(LOCK_MAP_NAME).setBackupCount(1);

    // Configure the specifics of each Hazelcast map.
    cfg.addMapConfig(createMapConfigForFactDaoCache(FACT_BY_HASH_MAP_NAME));
    cfg.addMapConfig(createMapConfigForFactDaoCache(FACT_BY_ID_MAP_NAME));
    cfg.addMapConfig(createMapConfigForObjectDaoCache(OBJECT_BY_ID_MAP_NAME));
    cfg.addMapConfig(createMapConfigForObjectDaoCache(OBJECT_BY_TYPE_VALUE_MAP_NAME));
  }

  private SerializerConfig createSerializerConfig(Class<?> type, int typeID) {
    return new SerializerConfig()
            .setTypeClass(type)
            .setImplementation(new HazelcastJsonSerializer<>(type, typeID));
  }

  private MapConfig createMapConfigForFactDaoCache(String name) {
    return new MapConfig(name)
            .setBackupCount(0) // Backup isn't needed as the data will simply be fetched again.
            .setTimeToLiveSeconds((int) daoCacheFactTimeToLive.getSeconds())
            .setMaxSizeConfig(new MaxSizeConfig().setSize(daoCacheFactMaximumCacheSize))
            .setEvictionPolicy(EvictionPolicy.LRU)
            .setNearCacheConfig(new NearCacheConfig()
                    .setTimeToLiveSeconds((int) daoCacheFactNearCacheTimeToLive.getSeconds())
                    .setEvictionConfig(new EvictionConfig()
                            .setSize(daoCacheFactNearCacheMaximumCacheSize)
                            .setEvictionPolicy(EvictionPolicy.LRU))
            );
  }

  private MapConfig createMapConfigForObjectDaoCache(String name) {
    return new MapConfig(name)
            .setBackupCount(0) // Backup isn't needed as the data will simply be fetched again.
            .setMaxIdleSeconds((int) daoCacheObjectTimeToIdle.getSeconds())
            .setMaxSizeConfig(new MaxSizeConfig().setSize(daoCacheObjectMaximumCacheSize))
            .setEvictionPolicy(EvictionPolicy.LFU)
            .setNearCacheConfig(new NearCacheConfig()
                    .setMaxIdleSeconds((int) daoCacheObjectNearCacheTimeToIdle.getSeconds())
                    .setEvictionConfig(new EvictionConfig()
                            .setSize(daoCacheObjectNearCacheMaximumCacheSize)
                            .setEvictionPolicy(EvictionPolicy.LFU))
            );
  }
}
