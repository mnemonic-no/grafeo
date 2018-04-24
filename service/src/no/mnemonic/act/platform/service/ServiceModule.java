package no.mnemonic.act.platform.service;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.auth.properties.module.PropertiesBasedAccessControllerModule;
import no.mnemonic.act.platform.dao.DaoModule;
import no.mnemonic.act.platform.service.aspects.AuthenticationAspect;
import no.mnemonic.act.platform.service.aspects.RequestContextAspect;
import no.mnemonic.act.platform.service.aspects.TriggerContextAspect;
import no.mnemonic.act.platform.service.aspects.ValidationAspect;
import no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl;
import no.mnemonic.act.platform.service.validators.DefaultValidatorFactory;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;

public class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new DaoModule());
    install(new PropertiesBasedAccessControllerModule());
    install(new AuthenticationAspect());
    install(new RequestContextAspect());
    install(new ValidationAspect());
    install(new TriggerContextAspect());
    // TODO: Bind TriggerEventConsumer to a real implementation.
    bind(TriggerEventConsumer.class).toInstance(event -> { });
    bind(ValidatorFactory.class).to(DefaultValidatorFactory.class).in(Scopes.SINGLETON);
    bind(ThreatIntelligenceService.class).to(ThreatIntelligenceServiceImpl.class).in(Scopes.SINGLETON);
  }

}
