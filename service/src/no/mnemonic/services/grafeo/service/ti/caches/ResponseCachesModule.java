package no.mnemonic.services.grafeo.service.ti.caches;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import no.mnemonic.services.grafeo.api.model.v1.*;

import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;

/**
 * Module which configures all response caches used in the service implementation.
 */
public class ResponseCachesModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Map<UUID, FactType>>() {})
            .toProvider(LocalFactTypeResponseCache.class)
            .in(Singleton.class);
    bind(new TypeLiteral<Map<UUID, ObjectType>>() {})
            .toProvider(LocalObjectTypeResponseCache.class)
            .in(Singleton.class);
    bind(new TypeLiteral<Map<UUID, Organization>>() {})
            .toProvider(LocalOrganizationResponseCache.class)
            .in(Singleton.class);
    bind(new TypeLiteral<Map<UUID, Origin>>() {})
            .toProvider(LocalOriginResponseCache.class)
            .in(Singleton.class);
    bind(new TypeLiteral<Map<UUID, Subject>>() {})
            .toProvider(LocalSubjectResponseCache.class)
            .in(Singleton.class);
  }
}
