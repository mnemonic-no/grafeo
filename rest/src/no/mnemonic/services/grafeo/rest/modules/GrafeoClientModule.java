package no.mnemonic.services.grafeo.rest.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.rest.client.GrafeoServiceProxyClient;

/**
 * Module which will access the GrafeoService via HTTP.
 */
public class GrafeoClientModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GrafeoService.class).toProvider(GrafeoServiceProxyClient.class).in(Scopes.SINGLETON);
  }
}
