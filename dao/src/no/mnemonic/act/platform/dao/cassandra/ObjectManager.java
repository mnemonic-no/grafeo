package no.mnemonic.act.platform.dao.cassandra;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectByTypeValueEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.dao.cassandra.mapper.ObjectDao;
import no.mnemonic.act.platform.dao.cassandra.mapper.ObjectTypeDao;
import no.mnemonic.act.platform.dao.cassandra.utilities.MultiFetchIterator;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class ObjectManager implements LifecycleAspect {

  @Dependency
  private final ClusterManager clusterManager;

  private final LoadingCache<UUID, ObjectTypeEntity> objectTypeByIdCache;
  private final LoadingCache<String, ObjectTypeEntity> objectTypeByNameCache;

  private ObjectTypeDao objectTypeDao;
  private ObjectDao objectDao;

  @Inject
  public ObjectManager(ClusterManager clusterManager) {
    this.clusterManager = clusterManager;
    this.objectTypeByIdCache = createObjectTypeByIdCache();
    this.objectTypeByNameCache = createObjectTypeByNameCache();
  }

  @Override
  public void startComponent() {
    objectTypeDao = clusterManager.getCassandraMapper().getObjectTypeDao();
    objectDao = clusterManager.getCassandraMapper().getObjectDao();
  }

  @Override
  public void stopComponent() {
    // NOOP
  }

  /* ObjectTypeEntity-related methods */

  public ObjectTypeEntity getObjectType(UUID id) {
    if (id == null) return null;

    try {
      return objectTypeByIdCache.get(id);
    } catch (ExecutionException ignored) {
      // If fetching ObjectType fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public ObjectTypeEntity getObjectType(String name) {
    if (StringUtils.isBlank(name)) return null;

    try {
      return objectTypeByNameCache.get(name);
    } catch (ExecutionException ignored) {
      // If fetching ObjectType fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public List<ObjectTypeEntity> fetchObjectTypes() {
    return objectTypeDao.fetch().all();
  }

  public ObjectTypeEntity saveObjectType(ObjectTypeEntity type) {
    if (type == null) return null;

    // It's not allowed to add an ObjectType with the same name, but if the IDs are equal this is updating an existing ObjectType.
    ObjectTypeEntity existing = getObjectType(type.getName());
    if (existing != null && !Objects.equals(existing.getId(), type.getId()) && Objects.equals(existing.getName(), type.getName())) {
      throw new IllegalArgumentException(String.format("ObjectType with name = %s already exists.", type.getName()));
    }

    objectTypeDao.save(type);
    objectTypeByIdCache.invalidate(type.getId());
    objectTypeByNameCache.invalidate(type.getName());

    return type;
  }

  /* ObjectEntity-related methods */

  public ObjectEntity getObject(UUID id) {
    if (id == null) return null;
    return objectDao.get(id);
  }

  public ObjectEntity getObject(String type, String value) {
    if (StringUtils.isBlank(type) || StringUtils.isBlank(value)) return null;

    ObjectTypeEntity objectType = getObjectType(type);
    if (objectType == null) throw new IllegalArgumentException(String.format("ObjectType with name = %s does not exist.", type));

    ObjectByTypeValueEntity objectByTypeValue = objectDao.getObjectByTypeValue(objectType.getId(), value);
    return ObjectUtils.ifNotNull(objectByTypeValue, o -> getObject(o.getObjectID()));
  }

  public Iterator<ObjectEntity> getObjects(List<UUID> id) {
    if (CollectionUtils.isEmpty(id)) return Collections.emptyIterator();
    return new MultiFetchIterator<>(partition -> objectDao.fetchByID(partition).iterator(), id);
  }

  public ObjectEntity saveObject(ObjectEntity object) {
    if (object == null) return null;

    ObjectTypeEntity type = getObjectType(object.getTypeID());
    if (type == null) throw new IllegalArgumentException(String.format("ObjectType with id = %s does not exist.", object.getTypeID()));

    // It's not allowed to create the same object multiple times.
    if (getObject(type.getName(), object.getValue()) != null) {
      throw new ImmutableViolationException("Object already exists.");
    }

    // Also save an ObjectByTypeValue mapping.
    ObjectByTypeValueEntity objectByTypeValue = new ObjectByTypeValueEntity()
            .setObjectTypeID(object.getTypeID())
            .setObjectValue(object.getValue())
            .setObjectID(object.getId());

    objectDao.save(object);
    objectDao.save(objectByTypeValue);

    return object;
  }

  /* ObjectFactBindingEntity-related methods */

  public Iterator<ObjectFactBindingEntity> fetchObjectFactBindings(UUID id) {
    if (id == null) return Collections.emptyIterator();
    return objectDao.fetchObjectFactBindings(id).iterator();
  }

  public ObjectFactBindingEntity saveObjectFactBinding(ObjectFactBindingEntity binding) {
    if (binding == null) return null;
    if (getObject(binding.getObjectID()) == null)
      throw new IllegalArgumentException(String.format("Object with id = %s does not exist.", binding.getObjectID()));
    if (objectDao.getObjectFactBinding(binding.getObjectID(), binding.getFactID()) != null)
      throw new ImmutableViolationException("It is not allowed to update an ObjectFactBinding.");

    objectDao.save(binding);

    return binding;
  }

  /* Private helper methods */

  private LoadingCache<UUID, ObjectTypeEntity> createObjectTypeByIdCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, ObjectTypeEntity>() {
              @Override
              public ObjectTypeEntity load(UUID key) throws Exception {
                return ObjectUtils.notNull(objectTypeDao.get(key), new Exception(String.format("ObjectType with id = %s does not exist.", key)));
              }
            });
  }

  private LoadingCache<String, ObjectTypeEntity> createObjectTypeByNameCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, ObjectTypeEntity>() {
              @Override
              public ObjectTypeEntity load(String key) throws Exception {
                return ObjectUtils.notNull(objectTypeDao.get(key), new Exception(String.format("ObjectType with name = %s does not exist.", key)));
              }
            });
  }

}
