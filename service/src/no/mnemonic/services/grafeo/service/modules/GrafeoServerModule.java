package no.mnemonic.services.grafeo.service.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.services.common.api.ServiceSessionFactory;
import no.mnemonic.services.common.api.proxy.serializer.Serializer;
import no.mnemonic.services.grafeo.service.container.GrafeoServiceProxyServer;
import no.mnemonic.services.grafeo.service.container.NoopServiceSessionFactory;
import no.mnemonic.services.grafeo.service.container.XStreamMessageSerializerProvider;

/**
 * Module which will make the GrafeoService available via HTTP.
 */
public class GrafeoServerModule extends AbstractModule {

  private boolean skipDefaultMessageSerializer;

  @Override
  protected void configure() {
    if (!skipDefaultMessageSerializer) {
      // Omit default MessageSerializer if the module is configured using withoutDefaultMessageSerializer().
      bind(Serializer.class).toProvider(XStreamMessageSerializerProvider.class);
    }

    // The service implementation doesn't have sessions, thus, just use a noop session.
    bind(ServiceSessionFactory.class).to(NoopServiceSessionFactory.class);
    // Bind server class which will make the GrafeoService available via HTTP.
    // It relies on the GrafeoServiceModule to provide the concrete service implementation.
    bind(GrafeoServiceProxyServer.class).in(Scopes.SINGLETON);
  }

  /**
   * Instruct the module to omit the default MessageSerializer implementation. In this case an alternative
   * implementation must be configured in Guice.
   *
   * @return this
   */
  public GrafeoServerModule withoutDefaultMessageSerializer() {
    this.skipDefaultMessageSerializer = true;
    return this;
  }
}
