package no.mnemonic.services.grafeo.dao.modules;

import com.google.inject.AbstractModule;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.facade.ObjectFactDaoFacade;
import no.mnemonic.services.grafeo.dao.facade.resolvers.CachedFactResolver;
import no.mnemonic.services.grafeo.dao.facade.resolvers.CachedObjectResolver;
import no.mnemonic.services.grafeo.dao.facade.resolvers.MapBackedFactResolver;
import no.mnemonic.services.grafeo.dao.facade.resolvers.MapBackedObjectResolver;

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
