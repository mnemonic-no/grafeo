package no.mnemonic.services.grafeo.rest.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.services.grafeo.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.services.grafeo.rest.client.SmbClient;

/**
 * Module which will access the ThreatIntelligenceService via the Service Message Bus.
 */
public class TiClientModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ThreatIntelligenceService.class).toProvider(SmbClient.class).in(Scopes.SINGLETON);
  }
}
