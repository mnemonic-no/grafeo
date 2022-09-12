package no.mnemonic.act.platform.dao.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.dao.elastic.ClientFactory;
import no.mnemonic.act.platform.dao.elastic.ClientFactoryProvider;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;

public class ElasticSearchModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ClientFactory.class).toProvider(ClientFactoryProvider.class).in(Scopes.SINGLETON);
    bind(FactSearchManager.class);
  }
}
