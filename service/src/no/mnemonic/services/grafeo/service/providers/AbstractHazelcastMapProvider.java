package no.mnemonic.services.grafeo.service.providers;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.NearCacheStats;
import no.mnemonic.commons.metrics.MetricAspect;
import no.mnemonic.commons.metrics.MetricException;
import no.mnemonic.commons.metrics.Metrics;
import no.mnemonic.commons.metrics.MetricsData;

import javax.inject.Provider;
import java.util.Map;

/**
 * Base class for a {@link Provider} which returns a {@link Map} instance backed by Hazelcast.
 * <p>
 * Implements collecting metrics from the underlying {@link IMap}.
 *
 * @param <K> Type of map key
 * @param <V> Type of map value
 */
public abstract class AbstractHazelcastMapProvider<K, V> implements Provider<Map<K, V>>, MetricAspect {

  private final HazelcastInstance hazelcastInstance;
  private final String mapName;

  protected AbstractHazelcastMapProvider(HazelcastInstance hazelcastInstance, String mapName) {
    this.hazelcastInstance = hazelcastInstance;
    this.mapName = mapName;
  }

  @Override
  public Map<K, V> get() {
    return hazelcastInstance.getMap(mapName);
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    IMap<K, V> map = (IMap<K, V>) get();

    LocalMapStats stats = map.getLocalMapStats();
    MetricsData metrics = new MetricsData()
            .addData("size", map.size())
            .addData("heapMemoryCost", stats.getHeapCost())
            .addData("ownedEntryCount", stats.getOwnedEntryCount())
            .addData("ownedEntryMemoryCost", stats.getOwnedEntryMemoryCost())
            .addData("backupEntryCount", stats.getBackupEntryCount())
            .addData("backupEntryMemoryCost", stats.getBackupEntryMemoryCost())
            .addData("hitCount", stats.getHits())
            .addData("putCount", stats.getPutOperationCount())
            .addData("putLatencyTotal", stats.getTotalPutLatency())
            .addData("putLatencyMax", stats.getMaxPutLatency())
            .addData("getCount", stats.getGetOperationCount())
            .addData("getLatencyTotal", stats.getTotalGetLatency())
            .addData("getLatencyMax", stats.getMaxGetLatency())
            .addData("removeCount", stats.getRemoveOperationCount())
            .addData("removeLatencyTotal", stats.getTotalRemoveLatency())
            .addData("removeLatencyMax", stats.getMaxRemoveLatency());

    if (stats.getNearCacheStats() != null) {
      NearCacheStats nearCacheStats = stats.getNearCacheStats();
      metrics.addData("nearCacheOwnedEntryCount", stats.getOwnedEntryCount())
              .addData("nearCacheOwnedEntryMemoryCost", stats.getOwnedEntryMemoryCost())
              .addData("nearCacheHitCount", nearCacheStats.getHits())
              .addData("nearCacheMissCount", nearCacheStats.getMisses())
              .addData("nearCacheEvictionCount", nearCacheStats.getEvictions())
              .addData("nearCacheExpirationCount", nearCacheStats.getExpirations())
              .addData("nearCacheInvalidationCount", nearCacheStats.getInvalidations());
    }

    return metrics;
  }
}
