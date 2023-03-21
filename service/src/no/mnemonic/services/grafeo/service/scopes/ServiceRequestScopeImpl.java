package no.mnemonic.services.grafeo.service.scopes;

import com.google.inject.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link ServiceRequestScope}. Enter the scope by calling {@link #enter()} and make sure to leave the
 * scope with {@link #exit()} when done. Use {@link #seed(Key, Object)} to explicitly seed classes for this scope. The
 * scope returns the same instance if requested multiple times.
 */
@SuppressWarnings("unchecked")
public class ServiceRequestScopeImpl implements Scope {

  private static final ThreadLocal<Map<Key<?>, Object>> seededClasses = new ThreadLocal<>();

  @Override
  public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
    return () -> {
      validateInScope();

      T current = (T) seededClasses.get().get(key);
      if (current == null) {
        // Not an explicitly seeded class, request new instance from Guice instead.
        current = unscoped.get();

        // Don't remember proxies, these exist only to serve circular dependencies.
        if (Scopes.isCircularProxy(current)) {
          return current;
        }

        // Remember instance for the next injection.
        seededClasses.get().put(key, current);
      }

      return current;
    };
  }

  @Override
  public String toString() {
    return "ServiceRequestScope";
  }

  /**
   * Explicitly seed Objects for the service request scope.
   *
   * @param key  Key to bind
   * @param seed Object bound to the key
   * @param <T>  Type of seeded object
   */
  public <T> void seed(Key<T> key, T seed) {
    validateInScope();

    if (seededClasses.get().containsKey(key)) {
      throw new IllegalStateException(String.format("A value for the key %s was already seeded in this scope.", key));
    }
    seededClasses.get().put(key, seed);
  }

  /**
   * Check whether the service request scope has been entered.
   *
   * @return True if scope has been entered
   */
  public boolean isInsideScope() {
    return seededClasses.get() != null;
  }

  /**
   * Start the service request scope.
   */
  public void enter() {
    if (isInsideScope()) throw new IllegalStateException("A scoping block is already in progress.");
    seededClasses.set(new HashMap<>());
  }

  /**
   * Leave the service request scope when done.
   */
  public void exit() {
    if (!isInsideScope()) throw new IllegalStateException("No scoping block in progress.");
    seededClasses.remove();
  }

  private void validateInScope() {
    if (!isInsideScope()) {
      throw new OutOfScopeException("Cannot access scope outside of a scoping block.");
    }
  }
}
