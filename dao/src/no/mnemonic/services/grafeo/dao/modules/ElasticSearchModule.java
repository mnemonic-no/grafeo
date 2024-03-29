package no.mnemonic.services.grafeo.dao.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.services.grafeo.dao.elastic.ClientFactory;
import no.mnemonic.services.grafeo.dao.elastic.ClientFactoryProvider;
import no.mnemonic.services.grafeo.dao.elastic.FactSearchManager;

public class ElasticSearchModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ClientFactory.class).toProvider(ClientFactoryProvider.class).in(Scopes.SINGLETON);
    bind(FactSearchManager.class);
  }
}
