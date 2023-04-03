package no.mnemonic.services.grafeo.service.caches;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Configuration providing a global cache for {@link ObjectType} information on one local node. Entries are cached across
 * multiple requests from different users.
 */
@Singleton
public class LocalObjectTypeResponseCache implements Provider<Map<UUID, ObjectType>> {

  private final Cache<UUID, ObjectType> cache = CacheBuilder.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build();

  @Override
  public Map<UUID, ObjectType> get() {
    return cache.asMap();
  }
}
