package no.mnemonic.act.platform.service.ti.caches;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.mnemonic.act.platform.api.model.v1.Origin;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Configuration providing a global cache for {@link Origin} information on one local node. Entries are cached across
 * multiple requests from different users.
 */
@Singleton
public class LocalOriginResponseCache implements Provider<Map<UUID, Origin>> {

  private final Cache<UUID, Origin> cache = CacheBuilder.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build();

  @Override
  public Map<UUID, Origin> get() {
    return cache.asMap();
  }
}
