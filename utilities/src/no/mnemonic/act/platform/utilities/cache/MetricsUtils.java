package no.mnemonic.act.platform.utilities.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import no.mnemonic.commons.metrics.MetricAspect;
import no.mnemonic.commons.metrics.MetricException;
import no.mnemonic.commons.metrics.MetricsData;

/**
 * Utility to collect metrics from components in a standardized fashion.
 */
public class MetricsUtils {

  /**
   * Collect metrics from a Guava {@link Cache} which can be returned directly from {@link MetricAspect}.
   * <p>
   * Note that the given {@link Cache} instance must have been initialized with statistics enabled.
   *
   * @param cache Instance to collect metrics from
   * @return Collected metrics
   * @throws MetricException Thrown when metrics collection failed
   */
  public static MetricsData collectCacheMetrics(Cache<?, ?> cache) throws MetricException {
    CacheStats stats = cache.stats();
    return new MetricsData()
            .addData("size", cache.size())
            .addData("requestCount", stats.requestCount())
            .addData("hitCount", stats.hitCount())
            .addData("missCount", stats.missCount())
            .addData("loadCount", stats.loadCount())
            .addData("loadSuccessCount", stats.loadSuccessCount())
            .addData("loadExceptionCount", stats.loadExceptionCount())
            .addData("evictionCount", stats.evictionCount())
            .addData("totalLoadTime", stats.totalLoadTime());
  }

  private MetricsUtils() {
  }
}
