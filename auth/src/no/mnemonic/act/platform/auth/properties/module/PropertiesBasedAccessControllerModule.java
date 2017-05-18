package no.mnemonic.act.platform.auth.properties.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.auth.properties.PropertiesBasedIdentityResolver;
import no.mnemonic.services.common.auth.AccessController;

public class PropertiesBasedAccessControllerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(IdentityResolver.class).to(PropertiesBasedIdentityResolver.class);
    bind(OrganizationResolver.class).toProvider(PropertiesBasedAccessControllerProvider.class);
    bind(SubjectResolver.class).toProvider(PropertiesBasedAccessControllerProvider.class);
    // Need to set Scopes.SINGLETON, otherwise the ComponentContainer won't pick up the LifecycleAspect.
    bind(AccessController.class).toProvider(PropertiesBasedAccessControllerProvider.class).in(Scopes.SINGLETON);
  }

}
