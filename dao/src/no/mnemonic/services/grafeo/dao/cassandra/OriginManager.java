package no.mnemonic.services.grafeo.dao.cassandra;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.dao.cassandra.mapper.OriginDao;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class OriginManager implements LifecycleAspect {

  @Dependency
  private final ClusterManager clusterManager;

  private final LoadingCache<UUID, OriginEntity> originByIdCache;
  private final LoadingCache<String, OriginEntity> originByNameCache;

  private OriginDao originDao;

  @Inject
  public OriginManager(ClusterManager clusterManager) {
    this.clusterManager = clusterManager;
    this.originByIdCache = createOriginByIdCache();
    this.originByNameCache = createOriginByNameCache();
  }

  @Override
  public void startComponent() {
    originDao = clusterManager.getCassandraMapper().getOriginDao();
  }

  @Override
  public void stopComponent() {
    // NOOP
  }

  public OriginEntity getOrigin(UUID id) {
    if (id == null) return null;

    try {
      return originByIdCache.get(id);
    } catch (ExecutionException ignored) {
      // If fetching Origin fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public OriginEntity getOrigin(String name) {
    if (StringUtils.isBlank(name)) return null;

    try {
      return originByNameCache.get(name);
    } catch (ExecutionException ignored) {
      // If fetching Origin fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public List<OriginEntity> fetchOrigins() {
    return originDao.fetch().all();
  }

  public OriginEntity saveOrigin(OriginEntity origin) {
    if (origin == null) return null;

    // It's not allowed to add an Origin with the same name, but if the IDs are equal this is updating an existing Origin.
    OriginEntity existing = getOrigin(origin.getName());
    if (existing != null && !Objects.equals(existing.getId(), origin.getId()) && Objects.equals(existing.getName(), origin.getName())) {
      throw new IllegalArgumentException(String.format("Origin with name = %s already exists.", origin.getName()));
    }

    originDao.save(origin);
    originByIdCache.invalidate(origin.getId());
    originByNameCache.invalidate(origin.getName());

    return origin;
  }

  private LoadingCache<UUID, OriginEntity> createOriginByIdCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, OriginEntity>() {
              @Override
              public OriginEntity load(UUID key) throws Exception {
                return ObjectUtils.notNull(originDao.get(key), new Exception(String.format("Origin with id = %s does not exist.", key)));
              }
            });
  }

  private LoadingCache<String, OriginEntity> createOriginByNameCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, OriginEntity>() {
              @Override
              public OriginEntity load(String key) throws Exception {
                return ObjectUtils.notNull(originDao.get(key), new Exception(String.format("Origin with name = %s does not exist.", key)));
              }
            });
  }

}
