package no.mnemonic.services.grafeo.rest.resteasy;

import com.google.inject.Binding;
import com.google.inject.Injector;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * A bridge between Guice and RESTEasy.
 * <p>
 * When the application starts up it will retrieve all JAX-RS resources and providers configured in Guice and
 * registers them within RESTEasy.
 */
public class GuiceResteasyBootstrapServletContextListener extends ResteasyBootstrap {

  private static final Logger LOGGER = Logging.getLogger(GuiceResteasyBootstrapServletContextListener.class);

  private final Injector injector;

  @Inject
  public GuiceResteasyBootstrapServletContextListener(Injector injector) {
    this.injector = injector;
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    super.contextInitialized(event);

    Registry registry = deployment.getRegistry();
    ResteasyProviderFactory providerFactory = deployment.getProviderFactory();

    // Loop through all Guice bindings and register resources (@Path annotated) and providers (@Provider annotated) within RESTEasy.
    for (Binding<?> binding : injector.getBindings().values()) {
      Class<?> beanClass = binding.getKey().getTypeLiteral().getRawType();
      if (beanClass.isAnnotationPresent(Path.class)) {
        LOGGER.info("Register resource %s", beanClass.getName());
        registry.addResourceFactory(new GuiceResteasyResourceFactory(binding.getProvider(), beanClass));
      }
      if (beanClass.isAnnotationPresent(Provider.class)) {
        LOGGER.info("Register provider %s", beanClass.getName());
        providerFactory.registerProviderInstance(binding.getProvider().get());
      }
    }
  }
}
