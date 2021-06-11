package no.mnemonic.act.platform.dao.cassandra;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.dao.cassandra.mapper.FactDao;
import no.mnemonic.act.platform.dao.cassandra.mapper.FactTypeDao;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@Singleton
public class FactManager implements LifecycleAspect {

  private static final Logger LOGGER = Logging.getLogger(FactManager.class);

  @Dependency
  private final ClusterManager clusterManager;

  private final LoadingCache<UUID, FactTypeEntity> factTypeByIdCache;
  private final LoadingCache<String, FactTypeEntity> factTypeByNameCache;

  private FactTypeDao factTypeDao;
  private FactDao factDao;

  private Clock clock = Clock.systemUTC();

  @Inject
  public FactManager(ClusterManager clusterManager) {
    this.clusterManager = clusterManager;
    this.factTypeByIdCache = createFactTypeByIdCache();
    this.factTypeByNameCache = createFactTypeByNameCache();
  }

  @Override
  public void startComponent() {
    factTypeDao = clusterManager.getCassandraMapper().getFactTypeDao();
    factDao = clusterManager.getCassandraMapper().getFactDao();
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
    return factTypeDao.fetch().all();
  }

  public FactTypeEntity saveFactType(FactTypeEntity type) {
    if (type == null) return null;

    // It's not allowed to add a FactType with the same name, but if the IDs are equal this is updating an existing FactType.
    FactTypeEntity existing = getFactType(type.getName());
    if (existing != null && !Objects.equals(existing.getId(), type.getId()) && Objects.equals(existing.getName(), type.getName())) {
      throw new IllegalArgumentException(String.format("FactType with name = %s already exists.", type.getName()));
    }

    factTypeDao.save(type);
    factTypeByIdCache.invalidate(type.getId());
    factTypeByNameCache.invalidate(type.getName());

    return type;
  }

  /* FactEntity-related methods */

  public FactEntity getFact(UUID id) {
    if (id == null) return null;
    return factDao.get(id);
  }

  public Iterator<FactEntity> getFactsWithin(long startTimestamp, long endTimestamp) {
    if (startTimestamp < 0 || endTimestamp < 0 || startTimestamp > endTimestamp)
      throw new IllegalArgumentException(String.format("Invalid startTimestamp %d or endTimestamp %d.", startTimestamp, endTimestamp));

    return new FactByTimestampIterator(startTimestamp, endTimestamp);
  }

  public FactEntity saveFact(FactEntity fact) {
    if (fact == null) return null;
    if (getFactType(fact.getTypeID()) == null)
      throw new IllegalArgumentException(String.format("FactType with id = %s does not exist.", fact.getTypeID()));
    if (getFact(fact.getId()) != null)
      throw new ImmutableViolationException("It is not allowed to update a Fact");

    factDao.save(fact);
    return fact;
  }

  public FactEntity refreshFact(UUID id) {
    FactEntity fact = getFact(id);
    if (fact == null) throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", id));
    factDao.save(fact.setLastSeenTimestamp(Instant.now(clock).toEpochMilli()));

    return fact;
  }

  public FactEntity retractFact(UUID id) {
    FactEntity fact = getFact(id);
    if (fact == null) throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", id));
    factDao.save(fact.addFlag(FactEntity.Flag.RetractedHint));

    return fact;
  }

  /* FactAclEntity-related methods */

  public List<FactAclEntity> fetchFactAcl(UUID id) {
    if (id == null) return ListUtils.list();
    return factDao.fetchAcl(id).all();
  }

  public FactAclEntity saveFactAclEntry(FactAclEntity entry) {
    if (entry == null) return null;
    if (getFact(entry.getFactID()) == null)
      throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", entry.getFactID()));
    if (factDao.getAclEntry(entry.getFactID(), entry.getId()) != null)
      throw new ImmutableViolationException("It is not allowed to update an ACL entry.");

    factDao.save(entry);

    return entry;
  }

  /* FactCommentEntity-related methods */

  public List<FactCommentEntity> fetchFactComments(UUID id) {
    if (id == null) return ListUtils.list();
    return factDao.fetchComments(id).all();
  }

  public FactCommentEntity saveFactComment(FactCommentEntity comment) {
    if (comment == null) return null;
    if (getFact(comment.getFactID()) == null)
      throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", comment.getFactID()));
    if (factDao.getComment(comment.getFactID(), comment.getId()) != null)
      throw new ImmutableViolationException("It is not allowed to update a comment.");

    factDao.save(comment);

    return comment;
  }

  /* MetaFactBindingEntity-related methods */

  public List<MetaFactBindingEntity> fetchMetaFactBindings(UUID id) {
    if (id == null) return ListUtils.list();
    return factDao.fetchMetaFactBindings(id).all();
  }

  public MetaFactBindingEntity saveMetaFactBinding(MetaFactBindingEntity binding) {
    if (binding == null) return null;
    if (getFact(binding.getFactID()) == null)
      throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", binding.getFactID()));
    if (factDao.getMetaFactBinding(binding.getFactID(), binding.getMetaFactID()) != null)
      throw new ImmutableViolationException("It is not allowed to update a MetaFactBinding.");

    factDao.save(binding);

    return binding;
  }

  /* FactByTimestampEntity-related methods */

  public FactByTimestampEntity saveFactByTimestamp(FactByTimestampEntity entity) {
    if (entity == null) return null;
    if (getFact(entity.getFactID()) == null)
      throw new IllegalArgumentException(String.format("Fact with id = %s does not exist.", entity.getFactID()));
    if (factDao.getFactByTimestamp(entity.getHourOfDay(), entity.getTimestamp(), entity.getFactID()) != null)
      throw new ImmutableViolationException("It is not allowed to update a FactByTimestamp entry.");

    factDao.save(entity);

    return entity;
  }

  /* Setters used for unit testing */

  FactManager withClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  /* Private helper methods and classes */

  /**
   * {@link Iterator} which uses the fact_by_timestamp table to look up Facts within a given timeframe.
   * It goes through all hourly buckets within the timeframe and fetches the Facts for each bucket.
   */
  private class FactByTimestampIterator implements Iterator<FactEntity> {

    private final long startTimestamp;
    private final long endTimestamp;
    private Instant currentBucket;
    private Iterator<FactEntity> currentBatch;

    private FactByTimestampIterator(long startTimestamp, long endTimestamp) {
      LOGGER.debug("Initialize FactByTimestampIterator for startTimestamp %s and endTimestamp %s.",
              Instant.ofEpochMilli(startTimestamp), Instant.ofEpochMilli(endTimestamp));

      this.startTimestamp = startTimestamp;
      this.endTimestamp = endTimestamp;

      // Calculate the first time bucket (truncate minutes, seconds, ...).
      currentBucket = Instant.ofEpochMilli(startTimestamp).truncatedTo(ChronoUnit.HOURS);
    }

    @Override
    public boolean hasNext() {
      // Handle initial batch.
      if (currentBatch == null) {
        currentBatch = nextBatch();
      }

      // One hourly bucket might yield zero elements. Therefore it needs to be skipped until the last bucket is reached.
      while (!currentBatch.hasNext() && currentBucket.toEpochMilli() < endTimestamp) {
        currentBatch = nextBatch();
      }

      return currentBatch.hasNext();
    }

    @Override
    public FactEntity next() {
      return currentBatch.next();
    }

    private Iterator<FactEntity> nextBatch() {
      LOGGER.debug("Fetch next batch from Cassandra for bucket %s.", currentBucket);

      // Fetch entities from the fact_by_timestamp lookup table for the current bucket and use the Fact IDs to fetch the actual data.
      Iterator<FactEntity> facts = StreamSupport.stream(factDao.fetchFactByTimestamp(currentBucket.toEpochMilli()).spliterator(), false)
              // Filter out entities which aren't within the given startTimestamp/endTimestamp interval. Note that
              // startTimestamp/endTimestamp might not be aligned with the current bucket, i.e. given with minutes, seconds...
              .filter(byTimestamp -> byTimestamp.getTimestamp() >= startTimestamp)
              .filter(byTimestamp -> byTimestamp.getTimestamp() < endTimestamp)
              .map(byTimestamp -> getFact(byTimestamp.getFactID()))
              .filter(Objects::nonNull)
              .iterator();

      // Advance to the next bucket for the next batch.
      currentBucket = currentBucket.plus(1, ChronoUnit.HOURS);

      return facts;
    }
  }

  private LoadingCache<UUID, FactTypeEntity> createFactTypeByIdCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, FactTypeEntity>() {
              @Override
              public FactTypeEntity load(UUID key) throws Exception {
                return ObjectUtils.notNull(factTypeDao.get(key), new Exception(String.format("FactType with id = %s does not exist.", key)));
              }
            });
  }

  private LoadingCache<String, FactTypeEntity> createFactTypeByNameCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, FactTypeEntity>() {
              @Override
              public FactTypeEntity load(String key) throws Exception {
                return ObjectUtils.notNull(factTypeDao.get(key), new Exception(String.format("FactType with name = %s does not exist.", key)));
              }
            });
  }

}
