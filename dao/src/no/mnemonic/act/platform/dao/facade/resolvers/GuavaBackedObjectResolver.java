package no.mnemonic.act.platform.dao.facade.resolvers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.facade.converters.ObjectRecordConverter;
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
 * {@link CachedObjectResolver} implementation which is backed by a node local {@link LoadingCache}.
 */
@Singleton
public class GuavaBackedObjectResolver implements CachedObjectResolver, MetricAspect {

  // Once written ObjectRecords never change, thus, time-to-idle semantics with expireAfterAccess() can be utilized.
  private static final Duration DEFAULT_TIME_TO_IDLE = Duration.ofMinutes(15);
  private static final long DEFAULT_MAXIMUM_CACHE_SIZE = 1_000_000;

  private final ObjectManager objectManager;
  private final ObjectRecordConverter objectRecordConverter;
  private final LoadingCache<UUID, ObjectRecord> objectByIdCache;
  private final LoadingCache<String, ObjectRecord> objectByTypeValueCache;

  @Inject
  public GuavaBackedObjectResolver(
          ObjectManager objectManager,
          ObjectRecordConverter objectRecordConverter,
          CacheConfiguration cacheConfiguration
  ) {
    this.objectManager = objectManager;
    this.objectRecordConverter = objectRecordConverter;
    this.objectByIdCache = createObjectByIdCache(cacheConfiguration);
    this.objectByTypeValueCache = createObjectByTypeValueCache(cacheConfiguration);
  }

  @Override
  public ObjectRecord getObject(UUID id) {
    if (id == null) return null;

    try {
      return objectByIdCache.get(id);
    } catch (ExecutionException ignored) {
      // Failed to fetch Object from underlying storage, return null instead.
      return null;
    }
  }

  @Override
  public ObjectRecord getObject(String type, String value) {
    if (StringUtils.isBlank(type) || StringUtils.isBlank(value)) return null;

    try {
      return objectByTypeValueCache.get(createCacheKey(type, value));
    } catch (ExecutionException ignored) {
      // Failed to fetch Object from underlying storage, return null instead.
      return null;
    }
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    return new MetricsGroup()
            .addSubMetrics("objectByIdCache", collectCacheMetrics(objectByIdCache))
            .addSubMetrics("objectByTypeValueCache", collectCacheMetrics(objectByTypeValueCache));
  }

  private LoadingCache<UUID, ObjectRecord> createObjectByIdCache(CacheConfiguration config) {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(config.getByIdTimeToIdle())
            .maximumSize(config.getByIdMaximumCacheSize())
            .recordStats()
            .build(new CacheLoader<UUID, ObjectRecord>() {
              @Override
              public ObjectRecord load(UUID key) throws Exception {
                ObjectEntity entity = objectManager.getObject(key);
                if (entity == null) throw new ElementNotFoundException();

                return objectRecordConverter.fromEntity(entity);
              }
            });
  }

  private LoadingCache<String, ObjectRecord> createObjectByTypeValueCache(CacheConfiguration config) {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(config.getByTypeValueTimeToIdle())
            .maximumSize(config.getByTypeValueMaximumCacheSize())
            .recordStats()
            .build(new CacheLoader<String, ObjectRecord>() {
              @Override
              public ObjectRecord load(String key) throws Exception {
                ObjectEntity entity = objectManager.getObject(extractType(key), extractValue(key));
                if (entity == null) throw new ElementNotFoundException();

                return objectRecordConverter.fromEntity(entity);
              }
            });
  }

  private String createCacheKey(String type, String value) {
    return type + "/" + value;
  }

  private String extractType(String key) {
    return key.substring(0, key.indexOf('/'));
  }

  private String extractValue(String key) {
    return key.substring(key.indexOf('/') + 1);
  }

  /**
   * Helper class to support optional configuration parameters but still instantiate the caches in the constructor.
   */
  public static final class CacheConfiguration {

    private Duration byIdTimeToIdle = DEFAULT_TIME_TO_IDLE;
    private Duration byTypeValueTimeToIdle = DEFAULT_TIME_TO_IDLE;
    private long byIdMaximumCacheSize = DEFAULT_MAXIMUM_CACHE_SIZE;
    private long byTypeValueMaximumCacheSize = DEFAULT_MAXIMUM_CACHE_SIZE;

    public Duration getByIdTimeToIdle() {
      return byIdTimeToIdle;
    }

    @Inject(optional = true)
    public CacheConfiguration setByIdTimeToIdle(@Named("act.dao.cache.objectById.ttl") long timeToIdle) {
      this.byIdTimeToIdle = Duration.ofMinutes(timeToIdle);
      return this;
    }

    public Duration getByTypeValueTimeToIdle() {
      return byTypeValueTimeToIdle;
    }

    @Inject(optional = true)
    public CacheConfiguration setByTypeValueTimeToIdle(@Named("act.dao.cache.objectByTypeValue.ttl") long timeToIdle) {
      this.byTypeValueTimeToIdle = Duration.ofMinutes(timeToIdle);
      return this;
    }

    public long getByIdMaximumCacheSize() {
      return byIdMaximumCacheSize;
    }

    @Inject(optional = true)
    public CacheConfiguration setByIdMaximumCacheSize(@Named("act.dao.cache.objectById.size") long maximumCacheSize) {
      this.byIdMaximumCacheSize = maximumCacheSize;
      return this;
    }

    public long getByTypeValueMaximumCacheSize() {
      return byTypeValueMaximumCacheSize;
    }

    @Inject(optional = true)
    public CacheConfiguration setByTypeValueMaximumCacheSize(@Named("act.dao.cache.objectByTypeValue.size") long maximumCacheSize) {
      this.byTypeValueMaximumCacheSize = maximumCacheSize;
      return this;
    }
  }

  /**
   * Exception to signal to a {@link CacheLoader} that an element could not be fetched from the underlying storage.
   * {@link LoadingCache} will wrap the exception inside an {@link ExecutionException} when calling get().
   */
  private static final class ElementNotFoundException extends Exception {
    private static final long serialVersionUID = -6509096511770828068L;
  }
}
