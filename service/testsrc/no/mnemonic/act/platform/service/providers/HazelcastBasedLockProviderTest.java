package no.mnemonic.act.platform.service.providers;

import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class HazelcastBasedLockProviderTest {

  private static HazelcastInstanceProvider hazelcastInstanceProvider;
  private HazelcastBasedLockProvider lockProvider;

  @BeforeClass
  public static void initialize() {
    hazelcastInstanceProvider = new HazelcastInstanceProvider(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "224.2.2.3",
            54327,
            false,
            new ActHazelcastConfiguration());
    hazelcastInstanceProvider.startComponent();
  }

  @AfterClass
  public static void shutdown() {
    if (hazelcastInstanceProvider != null) hazelcastInstanceProvider.stopComponent();
  }

  @Before
  public void setUp() {
    lockProvider = new HazelcastBasedLockProvider(hazelcastInstanceProvider.get())
            .setLockWaitTimeoutSeconds(1)
            .setLockLeaseTimeoutSeconds(2);
  }

  @Test
  public void testAcquireLock() {
    String region = UUID.randomUUID().toString();
    String key = UUID.randomUUID().toString();

    try (LockProvider.Lock ignored = lockProvider.acquireLock(region, key)) {
      assertTrue(lockProvider.isLocked(region, key));
    }

    assertFalse(lockProvider.isLocked(region, key));
  }

  @Test
  public void testAcquireLockForSameRegion() {
    String region = UUID.randomUUID().toString();
    String key1 = UUID.randomUUID().toString();
    String key2 = UUID.randomUUID().toString();

    try (LockProvider.Lock ignored1 = lockProvider.acquireLock(region, key1)) {
      assertTrue(lockProvider.isLocked(region, key1));
      assertFalse(lockProvider.isLocked(region, key2));

      try (LockProvider.Lock ignored2 = lockProvider.acquireLock(region, key2)) {
        assertTrue(lockProvider.isLocked(region, key1));
        assertTrue(lockProvider.isLocked(region, key2));
      }
    }

    assertFalse(lockProvider.isLocked(region, key1));
    assertFalse(lockProvider.isLocked(region, key2));
  }

  @Test
  public void testAcquireLockForDifferentRegion() {
    String region1 = UUID.randomUUID().toString();
    String region2 = UUID.randomUUID().toString();
    String key = UUID.randomUUID().toString();

    try (LockProvider.Lock ignored1 = lockProvider.acquireLock(region1, key)) {
      assertTrue(lockProvider.isLocked(region1, key));
      assertFalse(lockProvider.isLocked(region2, key));

      try (LockProvider.Lock ignored2 = lockProvider.acquireLock(region2, key)) {
        assertTrue(lockProvider.isLocked(region1, key));
        assertTrue(lockProvider.isLocked(region2, key));
      }
    }

    assertFalse(lockProvider.isLocked(region1, key));
    assertFalse(lockProvider.isLocked(region2, key));
  }

  @Test
  public void testReleasesLockOnException() {
    String region = UUID.randomUUID().toString();
    String key = UUID.randomUUID().toString();

    assertThrows(IllegalStateException.class, () -> {
      try (LockProvider.Lock ignored = lockProvider.acquireLock(region, key)) {
        assertTrue(lockProvider.isLocked(region, key));
        throw new IllegalStateException("Something bad happened!");
      }
    });

    assertFalse(lockProvider.isLocked(region, key));
  }

  @Test
  public void testReleasesLockAfterLeaseTimeout() throws Exception {
    String region = UUID.randomUUID().toString();
    String key = UUID.randomUUID().toString();
    lockProvider.acquireLock(region, key);
    assertTrue(lockProvider.isLocked(region, key));

    if (!LambdaUtils.waitFor(() -> !lockProvider.isLocked(region, key), 5, TimeUnit.SECONDS)) {
      fail("Lock did not get released automatically!");
    }

    assertFalse(lockProvider.isLocked(region, key));
  }

  @Test
  public void testAcquireLockWithInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> lockProvider.acquireLock(null, "key"));
    assertThrows(IllegalArgumentException.class, () -> lockProvider.acquireLock("", "key"));
    assertThrows(IllegalArgumentException.class, () -> lockProvider.acquireLock(" ", "key"));
    assertThrows(IllegalArgumentException.class, () -> lockProvider.acquireLock("region", null));
    assertThrows(IllegalArgumentException.class, () -> lockProvider.acquireLock("region", ""));
    assertThrows(IllegalArgumentException.class, () -> lockProvider.acquireLock("region", " "));
  }
}
