package no.mnemonic.services.grafeo.service.providers;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.metrics.*;
import no.mnemonic.commons.utilities.StringUtils;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link LockProvider} implementation based on a Hazelcast IMap.
 * <p>
 * This ensures that only one thread on one node will hold a lock at any given time. This is useful if code needs to be
 * synchronized across multiple nodes.
 */
@Singleton
public class HazelcastBasedLockProvider implements LockProvider, MetricAspect {

  private static final Logger LOGGER = Logging.getLogger(HazelcastBasedLockProvider.class);

  public static final String LOCK_MAP_NAME = "ACT.Service.Map.Lock";

  // By default, wait 10s to acquire a lock. If the lock isn't acquired within 10s acquireLock() will fail.
  private static final long DEFAULT_LOCK_WAIT_TIMEOUT_SECONDS = 10;
  // By default, set a lease timeout of 60s. If the lock isn't released within 60s Hazelcast will release it automatically.
  // This is a fail-safe, it should not happen if acquireLock() is used correctly.
  private static final long DEFAULT_LOCK_LEASE_TIMEOUT_SECONDS = 60;

  @Dependency
  private final HazelcastInstance hazelcastInstance;

  private final AtomicReference<IMap<String, Void>> lockMap = new AtomicReference<>();
  private final AtomicLong activeLocks = new AtomicLong();
  private final AtomicLong lockFailures = new AtomicLong();
  private final PerformanceMonitor lockWaitMonitor = new PerformanceMonitor(TimeUnit.MINUTES, 60, 1);

  private long lockWaitTimeoutSeconds = DEFAULT_LOCK_WAIT_TIMEOUT_SECONDS;
  private long lockLeaseTimeoutSeconds = DEFAULT_LOCK_LEASE_TIMEOUT_SECONDS;

  @Inject
  public HazelcastBasedLockProvider(HazelcastInstance hazelcastInstance) {
    this.hazelcastInstance = hazelcastInstance;
  }

  @Override
  public Lock acquireLock(String region, String key) {
    String mapKey = toMapKey(region, key);

    try (TimerContext ignored = TimerContext.timerMillis(lockWaitMonitor::invoked)) {
      LOGGER.debug("Try to acquire lock for key %s.", mapKey);
      if (!getLockMap().tryLock(mapKey, lockWaitTimeoutSeconds, TimeUnit.SECONDS, lockLeaseTimeoutSeconds, TimeUnit.SECONDS)) {
        lockFailures.incrementAndGet();
        throw new IllegalStateException(String.format("Failed to acquire lock for key %s within %ds.", mapKey, lockWaitTimeoutSeconds));
      }
    } catch (InterruptedException ex) {
      lockFailures.incrementAndGet();
      Thread.currentThread().interrupt();
      throw new IllegalStateException(String.format("Failed to acquire lock for key %s.", mapKey), ex);
    }

    LOGGER.debug("Successfully acquired lock for key %s.", mapKey);
    activeLocks.incrementAndGet();

    return () -> releaseLock(mapKey);
  }

  @Override
  public Metrics getMetrics() throws MetricException {
    return new MetricsData()
            .addData("activeLocks", activeLocks)
            .addData("lockFailures", lockFailures)
            .addData("lockWaitInvocations", lockWaitMonitor.getTotalInvocations())
            .addData("lockWaitTimeSpent", lockWaitMonitor.getTotalTimeSpent());
  }

  @Inject(optional = true)
  public HazelcastBasedLockProvider setLockWaitTimeoutSeconds(
          @Named("act.service.lock.provider.wait.timeout") long lockWaitTimeoutSeconds) {
    this.lockWaitTimeoutSeconds = lockWaitTimeoutSeconds;
    return this;
  }

  @Inject(optional = true)
  public HazelcastBasedLockProvider setLockLeaseTimeoutSeconds(
          @Named("act.service.lock.provider.lease.timeout") long lockLeaseTimeoutSeconds) {
    this.lockLeaseTimeoutSeconds = lockLeaseTimeoutSeconds;
    return this;
  }

  /**
   * Check whether a component already holds a lock for a given region and key. Should only be used by test code.
   *
   * @param region Identifies the lock region
   * @param key    Identifies the lock itself
   * @return Whether a key is locked
   */
  boolean isLocked(String region, String key) {
    return getLockMap().isLocked(toMapKey(region, key));
  }

  private void releaseLock(String key) {
    LOGGER.debug("Release lock for key %s.", key);
    activeLocks.decrementAndGet();

    getLockMap().unlock(key);
  }

  private String toMapKey(String region, String key) {
    if (StringUtils.isBlank(region) || StringUtils.isBlank(key))
      throw new IllegalArgumentException("Missing parameter 'region' or 'key'.");

    return region + "#" + key;
  }

  private IMap<String, Void> getLockMap() {
    // Lazily fetch map on first access.
    return lockMap.updateAndGet(existing -> existing != null ? existing : hazelcastInstance.getMap(LOCK_MAP_NAME));
  }
}
