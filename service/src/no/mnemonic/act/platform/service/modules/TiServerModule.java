package no.mnemonic.act.platform.service.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.service.container.NoopServiceSessionFactory;
import no.mnemonic.act.platform.service.container.SmbServer;
import no.mnemonic.services.common.api.ServiceSessionFactory;

/**
 * Module which will make the ThreatIntelligenceService available via the Service Message Bus.
 */
public class TiServerModule extends AbstractModule {

  @Override
  protected void configure() {
    // The service implementation doesn't have sessions, thus, just use a noop session for the SMB.
    bind(ServiceSessionFactory.class).to(NoopServiceSessionFactory.class);
    // Bind server class which will make the ThreatIntelligenceService available via SMB.
    // It relies on the TiServiceModule to provide the concrete service implementation.
    bind(SmbServer.class).in(Scopes.SINGLETON);
  }
}
