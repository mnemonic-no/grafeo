package no.mnemonic.act.platform.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.cassandra.*;
import no.mnemonic.act.platform.dao.elastic.ClientFactory;
import no.mnemonic.act.platform.dao.elastic.ClientFactoryProvider;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.facade.ObjectFactDaoFacade;
import no.mnemonic.act.platform.dao.facade.resolvers.CachedFactResolver;
import no.mnemonic.act.platform.dao.facade.resolvers.CachedObjectResolver;
import no.mnemonic.act.platform.dao.facade.resolvers.GuavaBackedFactResolver;
import no.mnemonic.act.platform.dao.facade.resolvers.GuavaBackedObjectResolver;

public class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    // Cassandra
    bind(ClusterManager.class).toProvider(ClusterManagerProvider.class).in(Scopes.SINGLETON);
    bind(FactManager.class);
    bind(ObjectManager.class);
    bind(OriginManager.class);

    // ElasticSearch
    bind(ClientFactory.class).toProvider(ClientFactoryProvider.class).in(Scopes.SINGLETON);
    bind(FactSearchManager.class);

    // Facade wrapping Cassandra + ElasticSearch
    bind(CachedObjectResolver.class).to(GuavaBackedObjectResolver.class).in(Scopes.SINGLETON);
    bind(CachedFactResolver.class).to(GuavaBackedFactResolver.class).in(Scopes.SINGLETON);
    bind(ObjectFactDao.class).to(ObjectFactDaoFacade.class);
  }

}
