package no.mnemonic.services.grafeo.service.providers;

import no.mnemonic.services.triggers.api.service.v1.TriggerAdministrationService;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import no.mnemonic.services.triggers.pipeline.worker.InMemoryQueueWorker;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

public class TriggerEventConsumerProvider implements Provider<TriggerEventConsumer> {

  // Use Provider such that Guice will only create an instance when actually needed.
  // This avoids loading the configuration files when action triggers is disabled.
  private final Provider<TriggerAdministrationService> triggerAdministrationServiceProvider;
  private final boolean enableActionTriggers;

  @Inject
  public TriggerEventConsumerProvider(
          Provider<TriggerAdministrationService> triggerAdministrationServiceProvider,
          @Named("grafeo.action.triggers.enabled") boolean enableActionTriggers) {
    this.triggerAdministrationServiceProvider = triggerAdministrationServiceProvider;
    this.enableActionTriggers = enableActionTriggers;
  }

  @Override
  public TriggerEventConsumer get() {
    if (enableActionTriggers) return new InMemoryQueueWorker(triggerAdministrationServiceProvider.get());

    // If disabled the consumer does nothing.
    return event -> {};
  }
}
