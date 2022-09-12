package no.mnemonic.act.platform.service.ti.caches;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.bindings.DaoCache;

import java.util.Map;
import java.util.UUID;

/**
 * Module which configures all DAO caches used in the service implementation.
 */
public class DaoCachesModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Map<UUID, ObjectRecord>>() {})
            .annotatedWith(DaoCache.class)
            .toProvider(DistributedObjectByIdDaoCache.class)
            .in(Scopes.SINGLETON);
    bind(new TypeLiteral<Map<String, ObjectRecord>>() {})
            .annotatedWith(DaoCache.class)
            .toProvider(DistributedObjectByTypeValueDaoCache.class)
            .in(Scopes.SINGLETON);
    bind(new TypeLiteral<Map<UUID, FactRecord>>() {})
            .annotatedWith(DaoCache.class)
            .toProvider(DistributedFactByIdDaoCache.class)
            .in(Scopes.SINGLETON);
    bind(new TypeLiteral<Map<String, UUID>>() {})
            .annotatedWith(DaoCache.class)
            .toProvider(DistributedFactByHashDaoCache.class)
            .in(Scopes.SINGLETON);
  }
}
