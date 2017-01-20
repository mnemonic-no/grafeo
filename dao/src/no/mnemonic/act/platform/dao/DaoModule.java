package no.mnemonic.act.platform.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.ClusterManagerProvider;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.handlers.DefaultEntityHandlerFactory;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;

public class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ClusterManager.class).toProvider(ClusterManagerProvider.class).in(Scopes.SINGLETON);
    bind(EntityHandlerFactory.class).to(DefaultEntityHandlerFactory.class).in(Scopes.SINGLETON);
    bind(FactManager.class);
    bind(ObjectManager.class);
  }

}
