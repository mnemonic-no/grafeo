package no.mnemonic.act.platform.service.providers;

/**
 * The LockProvider interface specifies a mechanism to acquire and release a lock for a small code block. The lock
 * semantics are specific to the LockProvider implementation. The implementation may choose to only synchronize code
 * execution between multiple threads in the same process, or may choose to synchronize code execution across multiple
 * processes running on distributed nodes.
 * <p>
 * The lock must be bound to a specific 'region' to avoid accidentally locking the same key twice. If the key is the
 * same but requested for a different region the calling code will successfully acquire the lock.
 * <p>
 * Usage example:
 * <pre>
 * {@code
 * try (LockProvider.Lock ignored = lockProvider.acquireLock("region", "key")) {
 *   doSomethingSynchronized();
 * }}
 * </pre>
 * {@link #acquireLock(String, String)} will acquire a lock for a given region and key and the lock will automatically
 * be released once the code execution leaves the try block. The code execution will stop until the lock is released
 * if another thread or node already holds the lock for a given region and key.
 */
public interface LockProvider {

  /**
   * Acquire a lock for a given region and key.
   *
   * @param region Identifies the lock region
   * @param key    Identifies the lock itself
   * @return Acquired lock
   */
  Lock acquireLock(String region, String key);

  /**
   * Lock returned from {@link #acquireLock(String, String)}. The lock should be released using {@link #close()}
   * once it is not required anymore, typically using a try-with-resource block.
   */
  interface Lock extends AutoCloseable {

    /**
     * Release a previously acquired lock.
     */
    @Override
    void close();
  }
}
