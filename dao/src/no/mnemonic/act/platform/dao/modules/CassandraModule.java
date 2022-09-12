package no.mnemonic.act.platform.dao.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.dao.cassandra.*;

public class CassandraModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ClusterManager.class).toProvider(ClusterManagerProvider.class).in(Scopes.SINGLETON);
    bind(FactManager.class);
    bind(ObjectManager.class);
    bind(OriginManager.class);
  }
}
