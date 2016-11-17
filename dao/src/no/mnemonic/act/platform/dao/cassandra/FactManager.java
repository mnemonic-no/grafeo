package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.driver.mapping.Mapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactAclAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactCommentAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactTypeAccessor;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.entity.cassandra.FactAclEntity;
import no.mnemonic.act.platform.entity.cassandra.FactCommentEntity;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class FactManager {

  private final EntityHandlerFactory entityHandlerFactory;
  private final Mapper<FactTypeEntity> factTypeMapper;
  private final Mapper<FactEntity> factMapper;
  private final Mapper<FactAclEntity> factAclMapper;
  private final Mapper<FactCommentEntity> factCommentMapper;
  private final FactTypeAccessor factTypeAccessor;
  private final FactAccessor factAccessor;
  private final FactAclAccessor factAclAccessor;
  private final FactCommentAccessor factCommentAccessor;

  private final LoadingCache<UUID, FactTypeEntity> factTypeByIdCache;
  private final LoadingCache<String, FactTypeEntity> factTypeByNameCache;

  private Clock clock = Clock.systemUTC();

  @Inject
  public FactManager(Provider<ClusterManager> provider, EntityHandlerFactory factory) {
    ClusterManager clusterManager = provider.get();
    entityHandlerFactory = factory;
    factTypeMapper = clusterManager.getMapper(FactTypeEntity.class);
    factMapper = clusterManager.getMapper(FactEntity.class);
    factAclMapper = clusterManager.getMapper(FactAclEntity.class);
    factCommentMapper = clusterManager.getMapper(FactCommentEntity.class);
    factTypeAccessor = clusterManager.getAccessor(FactTypeAccessor.class);
    factAccessor = clusterManager.getAccessor(FactAccessor.class);
    factAclAccessor = clusterManager.getAccessor(FactAclAccessor.class);
    factCommentAccessor = clusterManager.getAccessor(FactCommentAccessor.class);

    factTypeByIdCache = createFactTypeByIdCache();
    factTypeByNameCache = createFactTypeByNameCache();
  }

  /* FactTypeEntity-related methods */

  public FactTypeEntity getFactType(UUID id) {
    try {
      return factTypeByIdCache.get(id);
    } catch (ExecutionException ignored) {
      // If fetching FactType fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public FactTypeEntity getFactType(String name) {
    try {
      return factTypeByNameCache.get(name);
    } catch (ExecutionException ignored) {
      // If fetching FactType fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public List<FactTypeEntity> fetchFactTypes() {
    return factTypeAccessor.fetch().all();
  }

  public FactTypeEntity saveFactType(FactTypeEntity type) {
    if (type == null) return null;

    // It's not allowed to add a FactType with the same name, but if the IDs are equal this is updating an existing FactType.
    FactTypeEntity existing = getFactType(type.getName());
    if (existing != null && !existing.getId().equals(type.getId()) && existing.getName().equals(type.getName())) {
      throw new IllegalArgumentException(String.format("FactType with name = %s already exists.", type.getName()));
    }

    factTypeMapper.save(type);
    factTypeByIdCache.invalidate(type.getId());
    factTypeByNameCache.invalidate(type.getName());

    return type;
  }

  /* FactEntity-related methods */

  public FactEntity getFact(UUID id) {
    // Decode value using EntityHandler because it's stored encoded.
    return ObjectUtils.ifNotNull(factMapper.get(id), f -> f.setValue(decodeFactValue(getFactTypeOrFail(f.getTypeID()), f.getValue())));
  }

  public FactEntity saveFact(FactEntity fact) throws ImmutableViolationException {
    if (fact == null) return null;
    if (getFact(fact.getId()) != null) throw new ImmutableViolationException("It is not allowed to update a fact");

    // Encode value using EntityHandler to store value in encoded format.
    // Clone entity first in order to not change supplied fact instance.
    factMapper.save(fact.clone().setValue(encodeFactValue(getFactTypeOrFail(fact.getTypeID()), fact.getValue())));

    return fact;
  }

  public void refreshFact(UUID id) {
    if (getFact(id) == null) throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", id));
    factAccessor.refreshLastSeenTimestamp(id, Instant.now(clock).toEpochMilli());
  }

  /* FactAclEntity-related methods */

  public List<FactAclEntity> fetchFactAcl(UUID id) {
    return factAclAccessor.fetch(id).all();
  }

  public FactAclEntity saveFactAclEntry(FactAclEntity entry) throws ImmutableViolationException {
    if (entry == null) return null;
    if (getFact(entry.getFactID()) == null)
      throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", entry.getFactID()));
    if (factAclMapper.get(entry.getFactID(), entry.getId()) != null)
      throw new ImmutableViolationException("It is not allowed to update an ACL entry.");

    factAclMapper.save(entry);

    return entry;
  }

  /* FactCommentEntity-related methods */

  public List<FactCommentEntity> fetchFactComments(UUID id) {
    return factCommentAccessor.fetch(id).all();
  }

  public FactCommentEntity saveFactComment(FactCommentEntity comment) throws ImmutableViolationException {
    if (comment == null) return null;
    if (getFact(comment.getFactID()) == null)
      throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", comment.getFactID()));
    if (factCommentMapper.get(comment.getFactID(), comment.getId()) != null)
      throw new ImmutableViolationException("It is not allowed to update a comment.");

    factCommentMapper.save(comment);

    return comment;
  }

  /* Setters used for unit testing */

  FactManager withClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  /* Private helper methods */

  private LoadingCache<UUID, FactTypeEntity> createFactTypeByIdCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, FactTypeEntity>() {
              @Override
              public FactTypeEntity load(UUID key) throws Exception {
                return ObjectUtils.notNull(factTypeMapper.get(key), new Exception(String.format("FactType with id = %s does not exist.", key)));
              }
            });
  }

  private LoadingCache<String, FactTypeEntity> createFactTypeByNameCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, FactTypeEntity>() {
              @Override
              public FactTypeEntity load(String key) throws Exception {
                return ObjectUtils.notNull(factTypeAccessor.getByName(key), new Exception(String.format("FactType with name = %s does not exist.", key)));
              }
            });
  }

  private FactTypeEntity getFactTypeOrFail(UUID id) {
    try {
      return factTypeByIdCache.get(id);
    } catch (ExecutionException e) {
      throw new IllegalArgumentException(e.getCause());
    }
  }

  private String encodeFactValue(FactTypeEntity type, String value) {
    return entityHandlerFactory.get(type.getEntityHandler(), type.getEntityHandlerParameter()).encode(value);
  }

  private String decodeFactValue(FactTypeEntity type, String value) {
    return entityHandlerFactory.get(type.getEntityHandler(), type.getEntityHandlerParameter()).decode(value);
  }

}
