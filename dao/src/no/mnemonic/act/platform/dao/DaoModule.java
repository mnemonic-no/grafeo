package no.mnemonic.act.platform.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.ClusterManagerProvider;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.elastic.ClientFactory;
import no.mnemonic.act.platform.dao.elastic.ClientFactoryProvider;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;

public class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    // Cassandra
    bind(ClusterManager.class).toProvider(ClusterManagerProvider.class).in(Scopes.SINGLETON);
    bind(FactManager.class);
    bind(ObjectManager.class);

    // ElasticSearch
    bind(ClientFactory.class).toProvider(ClientFactoryProvider.class).in(Scopes.SINGLETON);
    bind(FactSearchManager.class);
  }

}
