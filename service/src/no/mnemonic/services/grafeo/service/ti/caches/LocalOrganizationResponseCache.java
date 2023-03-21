package no.mnemonic.services.grafeo.service.ti.caches;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.mnemonic.services.grafeo.api.model.v1.Organization;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Configuration providing a global cache for {@link Organization} information on one local node. Entries are cached across
 * multiple requests from different users.
 */
@Singleton
public class LocalOrganizationResponseCache implements Provider<Map<UUID, Organization>> {

  private final Cache<UUID, Organization> cache = CacheBuilder.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build();

  @Override
  public Map<UUID, Organization> get() {
    return cache.asMap();
  }
}
