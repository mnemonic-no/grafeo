package no.mnemonic.act.platform.dao.modules;

import com.google.inject.AbstractModule;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.facade.ObjectFactDaoFacade;
import no.mnemonic.act.platform.dao.facade.resolvers.CachedFactResolver;
import no.mnemonic.act.platform.dao.facade.resolvers.CachedObjectResolver;
import no.mnemonic.act.platform.dao.facade.resolvers.MapBackedFactResolver;
import no.mnemonic.act.platform.dao.facade.resolvers.MapBackedObjectResolver;

public class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new CassandraModule());
    install(new ElasticSearchModule());

    // Facade wrapping Cassandra + ElasticSearch
    bind(CachedObjectResolver.class).to(MapBackedObjectResolver.class);
    bind(CachedFactResolver.class).to(MapBackedFactResolver.class);
    bind(ObjectFactDao.class).to(ObjectFactDaoFacade.class);
  }
}
