package no.mnemonic.act.platform.dao.facade.resolvers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.facade.converters.FactRecordConverter;
import no.mnemonic.commons.metrics.MetricAspect;
import no.mnemonic.commons.metrics.MetricException;
import no.mnemonic.commons.metrics.Metrics;
import no.mnemonic.commons.metrics.MetricsGroup;
import no.mnemonic.commons.utilities.StringUtils;

import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static no.mnemonic.act.platform.utilities.cache.MetricsUtils.collectCacheMetrics;

/**
 * {@link CachedFactResolver} implementation which is backed by a node local {@link LoadingCache}.
 */
@Singleton
public class GuavaBackedFactResolver implements CachedFactResolver, MetricAspect {

  // As this is a node local cache a TTL of 5min means that there's potentially stale data for 5min on other nodes.
  // However, FactRecords aren't frequently updated and a lastSeenTimestamp which is slightly off is acceptable.
  private static final Duration DEFAULT_TIME_TO_LIVE = Duration.ofMinutes(5);
  private static final long DEFAULT_MAXIMUM_CACHE_SIZE = 500_000;

  private final FactManager factManager;
  private final FactRecordConverter factRecordConverter;
  private final LoadingCache<UUID, FactRecord> factByIdCache;
  private final LoadingCache<String, UUID> factByHashCache;

  @Inject
  public GuavaBackedFactResolver(
          FactManager factManager,
          FactRecordConverter factRecordConverter,
          CacheConfiguration cacheConfiguration
  ) {
    this.factManager = factManager;
    this.factRecordConverter = factRecordConverter;
    this.factByIdCache = createFactByIdCache(cacheConfiguration);
    this.factByHashCache = createFactByHashCache(cacheConfiguration);
  }

  @Override
  public FactRecord getFact(UUID id) {
    if (id == null) return null;

    try {
      return factByIdCache.get(id);
    } catch (ExecutionException ignored) {
      // Failed to fetch Fact from underlying storage, return null instead.
      return null;
    }
  }

  @Override
  public FactRecord getFact(String factHash) {
    if (StringUtils.isBlank(factHash)) return null;

    try {
      // Look up UUID from 'factByHashCache' and use the result to fetch the actual record from 'factByIdCache'.
      return factByIdCache.get(factByHashCache.get(factHash));
    } catch (ExecutionException ignored) {
      // Failed to fetch Fact from underlying storage, return null instead.
      return null;
    }
  }

  @Override
  public void evict(FactRecord fact) {
    if (fact == null || fact.getId() == null) return;
    factByIdCache.invalidate(fact.getId());
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    return new MetricsGroup()
            .addSubMetrics("factByIdCache", collectCacheMetrics(factByIdCache))
            .addSubMetrics("factByHashCache", collectCacheMetrics(factByHashCache));
  }

  private LoadingCache<UUID, FactRecord> createFactByIdCache(CacheConfiguration config) {
    return CacheBuilder.newBuilder()
            .expireAfterWrite(config.getByIdTimeToLive())
            .maximumSize(config.getByIdMaximumCacheSize())
            .recordStats()
            .build(new CacheLoader<UUID, FactRecord>() {
              @Override
              public FactRecord load(UUID key) throws Exception {
                FactEntity entity = factManager.getFact(key);
                if (entity == null) throw new ElementNotFoundException();

                return factRecordConverter.fromEntity(entity);
              }
            });
  }

  private LoadingCache<String, UUID> createFactByHashCache(CacheConfiguration config) {
    return CacheBuilder.newBuilder()
            .expireAfterWrite(config.getByHashTimeToLive())
            .maximumSize(config.getByHashMaximumCacheSize())
            .recordStats()
            .build(new CacheLoader<String, UUID>() {
              @Override
              public UUID load(String key) throws Exception {
                FactEntity entity = factManager.getFact(key);
                if (entity == null) throw new ElementNotFoundException();

                return entity.getId();
              }
            });
  }

  /**
   * Helper class to support optional configuration parameters but still instantiate the cache in the constructor.
   */
  public static final class CacheConfiguration {

    private Duration byIdTimeToLive = DEFAULT_TIME_TO_LIVE;
    private Duration byHashTimeToLive = DEFAULT_TIME_TO_LIVE;
    private long byIdMaximumCacheSize = DEFAULT_MAXIMUM_CACHE_SIZE;
    private long byHashMaximumCacheSize = DEFAULT_MAXIMUM_CACHE_SIZE;

    public Duration getByIdTimeToLive() {
      return byIdTimeToLive;
    }

    @Inject(optional = true)
    public CacheConfiguration setByIdTimeToLive(@Named("act.dao.cache.factById.ttl") long timeToLive) {
      this.byIdTimeToLive = Duration.ofMinutes(timeToLive);
      return this;
    }

    public Duration getByHashTimeToLive() {
      return byHashTimeToLive;
    }

    @Inject(optional = true)
    public CacheConfiguration setByHashTimeToLive(@Named("act.dao.cache.factByHash.ttl") long timeToLive) {
      this.byHashTimeToLive = Duration.ofMinutes(timeToLive);
      return this;
    }

    public long getByIdMaximumCacheSize() {
      return byIdMaximumCacheSize;
    }

    @Inject(optional = true)
    public CacheConfiguration setByIdMaximumCacheSize(@Named("act.dao.cache.factById.size") long maximumCacheSize) {
      this.byIdMaximumCacheSize = maximumCacheSize;
      return this;
    }

    public long getByHashMaximumCacheSize() {
      return byHashMaximumCacheSize;
    }

    @Inject(optional = true)
    public CacheConfiguration setByHashMaximumCacheSize(@Named("act.dao.cache.factByHash.size") long maximumCacheSize) {
      this.byHashMaximumCacheSize = maximumCacheSize;
      return this;
    }
  }

  /**
   * Exception to signal to a {@link CacheLoader} that an element could not be fetched from the underlying storage.
   * {@link LoadingCache} will wrap the exception inside an {@link ExecutionException} when calling get().
   */
  private static final class ElementNotFoundException extends Exception {
    private static final long serialVersionUID = -6417051307537158152L;
  }
}
