package no.mnemonic.services.grafeo.rest.resteasy;

import org.jboss.resteasy.spi.*;

import jakarta.inject.Provider;
import java.util.concurrent.CompletionStage;

/**
 * {@link ResourceFactory} which creates JAX-RS resources configured in Guice.
 */
public class GuiceResteasyResourceFactory implements ResourceFactory {

  private final Provider<?> provider;
  private final Class<?> scannableClass;
  private PropertyInjector propertyInjector;

  public GuiceResteasyResourceFactory(Provider<?> provider, Class<?> scannableClass) {
    this.provider = provider;
    this.scannableClass = scannableClass;
  }

  @Override
  public Class<?> getScannableClass() {
    return scannableClass;
  }

  @Override
  public void registered(ResteasyProviderFactory factory) {
    // Only create PropertyInjector once and reuse the same instance afterwards.
    this.propertyInjector = factory.getInjectorFactory().createPropertyInjector(scannableClass, factory);
  }

  @Override
  public Object createResource(HttpRequest request, HttpResponse response, ResteasyProviderFactory factory) {
    // Fetch a new instance of the requested resource from Guice.
    Object resource = provider.get();
    // Inject context variables into the resource.
    CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, resource, true);
    // If it's an asynchronous operation defer returning the resource. Otherwise, return the resource directly.
    return propertyStage == null ? resource : propertyStage.thenApply(v -> resource);
  }

  @Override
  public void requestFinished(HttpRequest request, HttpResponse response, Object resource) {
    // Noop
  }

  @Override
  public void unregistered() {
    // Noop
  }
}
