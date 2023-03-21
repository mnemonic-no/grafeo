package no.mnemonic.services.grafeo.rest.resteasy;

import org.jboss.resteasy.core.ResteasyContext;

import javax.inject.Provider;

/**
 * Simple {@link Provider} which makes data stored in {@link ResteasyContext} available for injection.
 *
 * @param <T> Type of context data
 */
public class GuiceResteasyContextDataProvider<T> implements Provider<T> {

  private final Class<T> instanceClass;

  public GuiceResteasyContextDataProvider(Class<T> instanceClass) {
    this.instanceClass = instanceClass;
  }

  @Override
  public T get() {
    return ResteasyContext.getRequiredContextData(instanceClass);
  }
}
