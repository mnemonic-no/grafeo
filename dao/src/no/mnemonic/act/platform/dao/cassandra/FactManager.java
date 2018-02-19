package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.driver.mapping.Mapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterators;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactAclAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactCommentAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.FactTypeAccessor;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactCommentEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.dao.handlers.EntityHandler;
import no.mnemonic.act.platform.dao.handlers.EntityHandlerFactory;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.datastax.driver.mapping.Mapper.Option.saveNullFields;

@Singleton
public class FactManager implements LifecycleAspect {

  @Dependency
  private final ClusterManager clusterManager;

  private final EntityHandlerFactory entityHandlerFactory;
  private final LoadingCache<UUID, FactTypeEntity> factTypeByIdCache;
  private final LoadingCache<String, FactTypeEntity> factTypeByNameCache;

  private Mapper<FactTypeEntity> factTypeMapper;
  private Mapper<FactEntity> factMapper;
  private Mapper<FactAclEntity> factAclMapper;
  private Mapper<FactCommentEntity> factCommentMapper;
  private FactTypeAccessor factTypeAccessor;
  private FactAccessor factAccessor;
  private FactAclAccessor factAclAccessor;
  private FactCommentAccessor factCommentAccessor;

  private Clock clock = Clock.systemUTC();

  @Inject
  public FactManager(ClusterManager clusterManager, EntityHandlerFactory factory) {
    this.clusterManager = clusterManager;
    this.entityHandlerFactory = factory;
    this.factTypeByIdCache = createFactTypeByIdCache();
    this.factTypeByNameCache = createFactTypeByNameCache();
  }

  @Override
  public void startComponent() {
    factTypeMapper = clusterManager.getMapper(FactTypeEntity.class);
    factMapper = clusterManager.getMapper(FactEntity.class);
    factAclMapper = clusterManager.getMapper(FactAclEntity.class);
    factCommentMapper = clusterManager.getMapper(FactCommentEntity.class);
    factTypeAccessor = clusterManager.getAccessor(FactTypeAccessor.class);
    factAccessor = clusterManager.getAccessor(FactAccessor.class);
    factAclAccessor = clusterManager.getAccessor(FactAclAccessor.class);
    factCommentAccessor = clusterManager.getAccessor(FactCommentAccessor.class);

    // Avoid creating tombstones for null values.
    factTypeMapper.setDefaultSaveOptions(saveNullFields(false));
    factMapper.setDefaultSaveOptions(saveNullFields(false));
    factAclMapper.setDefaultSaveOptions(saveNullFields(false));
    factCommentMapper.setDefaultSaveOptions(saveNullFields(false));
  }

  @Override
  public void stopComponent() {
    // NOOP
  }

  /* FactTypeEntity-related methods */

  public FactTypeEntity getFactType(UUID id) {
    if (id == null) return null;

    try {
      return factTypeByIdCache.get(id);
    } catch (ExecutionException ignored) {
      // If fetching FactType fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public FactTypeEntity getFactType(String name) {
    if (StringUtils.isBlank(name)) return null;

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
    if (id == null) return null;
    // Decode value using EntityHandler because it's stored encoded.
    return ObjectUtils.ifNotNull(factMapper.get(id), this::decodeFactValue);
  }

  public Iterator<FactEntity> getFacts(List<UUID> id) {
    if (CollectionUtils.isEmpty(id)) return Collections.emptyIterator();
    // Need to decode values using EntityHandler because they're stored encoded.
    // Use Iterators.transform() to do this lazily when facts are pulled from Cassandra.
    return Iterators.transform(factAccessor.fetchByID(id).iterator(), this::decodeFactValue);
  }

  public FactEntity saveFact(FactEntity fact) {
    if (fact == null) return null;
    if (getFact(fact.getId()) != null) throw new ImmutableViolationException("It is not allowed to update a fact");

    // Encode value using EntityHandler to store value in encoded format.
    // Clone entity first in order to not change supplied fact instance.
    factMapper.save(encodeFactValue(fact.clone()));

    return fact;
  }

  public FactEntity refreshFact(UUID id) {
    if (getFact(id) == null) throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", id));
    factAccessor.refreshLastSeenTimestamp(id, Instant.now(clock).toEpochMilli());

    return getFact(id);
  }

  /* FactAclEntity-related methods */

  public List<FactAclEntity> fetchFactAcl(UUID id) {
    if (id == null) return ListUtils.list();
    return factAclAccessor.fetch(id).all();
  }

  public FactAclEntity saveFactAclEntry(FactAclEntity entry) {
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
    if (id == null) return ListUtils.list();
    return factCommentAccessor.fetch(id).all();
  }

  public FactCommentEntity saveFactComment(FactCommentEntity comment) {
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

  private FactEntity encodeFactValue(FactEntity fact) {
    FactTypeEntity type = getFactTypeOrFail(fact.getTypeID());
    EntityHandler handler = entityHandlerFactory.get(type.getEntityHandler(), type.getEntityHandlerParameter());
    return fact.setValue(handler.encode(fact.getValue()));
  }

  private FactEntity decodeFactValue(FactEntity fact) {
    FactTypeEntity type = getFactTypeOrFail(fact.getTypeID());
    EntityHandler handler = entityHandlerFactory.get(type.getEntityHandler(), type.getEntityHandlerParameter());
    return fact.setValue(handler.decode(fact.getValue()));
  }

}
