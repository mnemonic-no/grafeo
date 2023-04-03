package no.mnemonic.services.grafeo.service.caches;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.mnemonic.services.grafeo.api.model.v1.Subject;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Configuration providing a global cache for {@link Subject} information on one local node. Entries are cached across
 * multiple requests from different users.
 */
@Singleton
public class LocalSubjectResponseCache implements Provider<Map<UUID, Subject>> {

  private final Cache<UUID, Subject> cache = CacheBuilder.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build();

  @Override
  public Map<UUID, Subject> get() {
    return cache.asMap();
  }
}
