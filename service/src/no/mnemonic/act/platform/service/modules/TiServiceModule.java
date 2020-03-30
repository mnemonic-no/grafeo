package no.mnemonic.act.platform.service.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.auth.properties.module.PropertiesBasedAccessControllerModule;
import no.mnemonic.act.platform.dao.DaoModule;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.service.aspects.*;
import no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl;
import no.mnemonic.act.platform.service.validators.DefaultValidatorFactory;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.services.triggers.api.service.v1.TriggerAdministrationService;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import no.mnemonic.services.triggers.pipeline.worker.InMemoryQueueWorker;
import no.mnemonic.services.triggers.service.TriggerAdministrationServiceImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

/**
 * Module which configures the implementation of the ThreatIntelligenceService.
 */
public class TiServiceModule extends AbstractModule {

  private boolean skipDefaultAccessController;

  @Override
  protected void configure() {
    // Install all dependencies for the service.
    install(new DaoModule());
    install(new AuthenticationAspect());
    install(new ValidationAspect());
    install(new TriggerContextAspect());
    install(new ServiceRequestScopeAspect());

    if (!skipDefaultAccessController) {
      // Omit default access controller if the module is configured using withoutDefaultAccessController().
      install(new PropertiesBasedAccessControllerModule());
    }

    // Configure the ActionTriggers' pipeline worker and administration service.
    bind(TriggerEventConsumer.class).to(InMemoryQueueWorker.class).in(Scopes.SINGLETON);
    bind(TriggerAdministrationService.class).to(TriggerAdministrationServiceImpl.class).in(Scopes.SINGLETON);

    // Bind the concrete implementation classes of the ThreatIntelligenceService.
    bind(ValidatorFactory.class).to(DefaultValidatorFactory.class).in(Scopes.SINGLETON);
    bind(ThreatIntelligenceService.class).to(ThreatIntelligenceServiceImpl.class).in(Scopes.SINGLETON);
  }

  @Provides
  Function<UUID, Collection<ObjectStatisticsContainer.FactStatistic>> provideFactStatisticsResolver() {
    // Don't include statistics in the default ObjectConverter, e.g. when including Objects as part of Facts.
    // When statistics should be included in responses create a new instance of the ObjectConverter instead.
    return id -> Collections.emptyList();
  }

  /**
   * Instruct the module to omit the default access controller implementation. In this case an alternative
   * implementation must be configured in Guice.
   *
   * @return this
   */
  public TiServiceModule withoutDefaultAccessController() {
    this.skipDefaultAccessController = true;
    return this;
  }
}
