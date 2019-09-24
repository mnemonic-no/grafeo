package no.mnemonic.act.platform.service.modules;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;

import java.util.UUID;
import java.util.function.Function;

/**
 * Module which configures the resolvers used by the service implementation.
 */
class ResolverModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Function<UUID, OriginEntity>>() {
    }).to(OriginResolver.class);
  }
}
