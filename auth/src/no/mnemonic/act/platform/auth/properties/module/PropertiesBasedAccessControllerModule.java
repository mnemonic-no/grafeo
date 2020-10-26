package no.mnemonic.act.platform.auth.properties.module;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.auth.IdentitySPI;
import no.mnemonic.act.platform.auth.OrganizationSPI;
import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.auth.SubjectSPI;
import no.mnemonic.act.platform.auth.properties.PropertiesBasedIdentityResolver;
import no.mnemonic.act.platform.auth.properties.PropertiesBasedServiceAccountCredentialsResolver;
import no.mnemonic.services.common.auth.AccessController;

public class PropertiesBasedAccessControllerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(IdentitySPI.class).to(PropertiesBasedIdentityResolver.class);
    bind(ServiceAccountSPI.class).to(PropertiesBasedServiceAccountCredentialsResolver.class);
    bind(OrganizationSPI.class).toProvider(PropertiesBasedAccessControllerProvider.class);
    bind(SubjectSPI.class).toProvider(PropertiesBasedAccessControllerProvider.class);
    // Need to set Scopes.SINGLETON, otherwise the ComponentContainer won't pick up the LifecycleAspect.
    bind(AccessController.class).toProvider(PropertiesBasedAccessControllerProvider.class).in(Scopes.SINGLETON);
  }

}
