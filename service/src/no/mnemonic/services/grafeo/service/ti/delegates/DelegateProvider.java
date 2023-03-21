package no.mnemonic.services.grafeo.service.ti.delegates;

import com.google.inject.Injector;

import javax.inject.Inject;

/**
 * Provider used to get delegate instances. It uses Guice and injects all dependencies into delegates.
 */
public class DelegateProvider {

  private final Injector injector;

  @Inject
  public DelegateProvider(Injector injector) {
    this.injector = injector;
  }

  /**
   * Fetch a delegate instance from Guice.
   *
   * @param delegateClass Class of delegate
   * @param <T>           Type of delegate (must extend {@link Delegate})
   * @return New delegate instance
   */
  public <T extends Delegate> T get(Class<T> delegateClass) {
    return injector.getInstance(delegateClass);
  }
}
