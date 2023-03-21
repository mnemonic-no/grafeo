package no.mnemonic.services.grafeo.cli.tools.commands;

import com.google.inject.Module;
import no.mnemonic.commons.container.ComponentContainer;
import no.mnemonic.commons.container.providers.BeanProvider;
import no.mnemonic.commons.container.providers.GuiceBeanProvider;

/**
 * Wrapper around {@link ComponentContainer} which allows commands to be executed inside a started container. Uses a Guice
 * {@link Module} to set up the container with all required dependencies. Dependencies handled by the container can be
 * injected into commands. If the command is handled by the container as well the container ensures that all dependencies
 * injected into that command are started before the command is executed.
 */
class ComponentContainerWrapper {

  private final BeanProvider beanProvider;

  ComponentContainerWrapper(Module module) {
    beanProvider = new GuiceBeanProvider(module);
  }

  /**
   * Return a bean handled by the {@link ComponentContainer}.
   *
   * @param ofType Class definition of requested bean
   * @param <T>    Type of requested bean
   * @return Requested bean
   */
  <T> T getBean(Class<T> ofType) {
    return beanProvider.getBean(ofType)
            .orElseThrow(() -> new IllegalStateException(String.format("Cannot resolve bean of type %s.", ofType.getSimpleName())));
  }

  /**
   * Execute a command inside a {@link ComponentContainer} instance.
   * <p>
   * Initializes a {@link ComponentContainer} and starts all managed beans in the correct order. Once the container is
   * initialized the given command is executed, and the container destroyed afterwards.
   *
   * @param command Command to execute
   */
  void execute(Runnable command) {
    ComponentContainer container = null;

    try {
      container = ComponentContainer.create(beanProvider);
      container.initialize();

      command.run();
    } finally {
      if (container != null) {
        container.destroy();
      }
    }
  }
}
