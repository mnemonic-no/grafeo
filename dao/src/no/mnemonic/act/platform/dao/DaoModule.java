package no.mnemonic.act.platform.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.ClusterManagerProvider;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.elastic.ClientFactory;
import no.mnemonic.act.platform.dao.elastic.ClientFactoryProvider;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.resolver.EntityHandlerForTypeIdResolver;
import no.mnemonic.act.platform.dao.handlers.DefaultEntityHandlerFactory;
import no.mnemonic.act.platform.dao.handlers.EntityHandler;
import no.mnemonic.act.platform.dao.handlers.EntityHandlerFactory;

import java.util.UUID;
import java.util.function.Function;

public class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    // Cassandra
    bind(ClusterManager.class).toProvider(ClusterManagerProvider.class).in(Scopes.SINGLETON);
    bind(EntityHandlerFactory.class).to(DefaultEntityHandlerFactory.class).in(Scopes.SINGLETON);
    bind(FactManager.class);
    bind(ObjectManager.class);

    // ElasticSearch
    bind(ClientFactory.class).toProvider(ClientFactoryProvider.class).in(Scopes.SINGLETON);
    bind(new TypeLiteral<Function<UUID, EntityHandler>>() {}).to(EntityHandlerForTypeIdResolver.class);
    bind(FactSearchManager.class);
  }

}
