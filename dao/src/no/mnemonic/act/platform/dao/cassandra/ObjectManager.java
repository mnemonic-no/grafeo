package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.driver.mapping.Mapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.act.platform.dao.cassandra.accessors.ObjectAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.ObjectTypeAccessor;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.entity.cassandra.ObjectByTypeValueEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class ObjectManager {

  private final EntityHandlerFactory entityHandlerFactory;
  private final Mapper<ObjectTypeEntity> objectTypeMapper;
  private final Mapper<ObjectEntity> objectMapper;
  private final Mapper<ObjectByTypeValueEntity> objectByTypeValueMapper;
  private final Mapper<ObjectFactBindingEntity> objectFactBindingMapper;
  private final ObjectTypeAccessor objectTypeAccessor;
  private final ObjectAccessor objectAccessor;

  private final LoadingCache<UUID, ObjectTypeEntity> objectTypeByIdCache;
  private final LoadingCache<String, ObjectTypeEntity> objectTypeByNameCache;

  @Inject
  public ObjectManager(Provider<ClusterManager> provider, EntityHandlerFactory factory) {
    ClusterManager clusterManager = provider.get();
    entityHandlerFactory = factory;
    objectTypeMapper = clusterManager.getMapper(ObjectTypeEntity.class);
    objectMapper = clusterManager.getMapper(ObjectEntity.class);
    objectByTypeValueMapper = clusterManager.getMapper(ObjectByTypeValueEntity.class);
    objectFactBindingMapper = clusterManager.getMapper(ObjectFactBindingEntity.class);
    objectTypeAccessor = clusterManager.getAccessor(ObjectTypeAccessor.class);
    objectAccessor = clusterManager.getAccessor(ObjectAccessor.class);

    objectTypeByIdCache = createObjectTypeByIdCache();
    objectTypeByNameCache = createObjectTypeByNameCache();
  }

  /* ObjectTypeEntity-related methods */

  public ObjectTypeEntity getObjectType(UUID id) {
    try {
      return objectTypeByIdCache.get(id);
    } catch (ExecutionException ignored) {
      // If fetching ObjectType fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public ObjectTypeEntity getObjectType(String name) {
    try {
      return objectTypeByNameCache.get(name);
    } catch (ExecutionException ignored) {
      // If fetching ObjectType fails just return null in order to be consistent with Cassandra's get().
      return null;
    }
  }

  public List<ObjectTypeEntity> fetchObjectTypes() {
    return objectTypeAccessor.fetch().all();
  }

  public ObjectTypeEntity saveObjectType(ObjectTypeEntity type) {
    if (type == null) return null;

    // It's not allowed to add an ObjectType with the same name, but if the IDs are equal this is updating an existing ObjectType.
    ObjectTypeEntity existing = getObjectType(type.getName());
    if (existing != null && !existing.getId().equals(type.getId()) && existing.getName().equals(type.getName())) {
      throw new IllegalArgumentException(String.format("ObjectType with name = %s already exists.", type.getName()));
    }

    objectTypeMapper.save(type);
    objectTypeByIdCache.invalidate(type.getId());
    objectTypeByNameCache.invalidate(type.getName());

    return type;
  }

  /* ObjectEntity-related methods */

  public ObjectEntity getObject(UUID id) {
    // Decode value using EntityHandler because it's stored encoded.
    return ObjectUtils.ifNotNull(objectMapper.get(id), o -> o.setValue(decodeObjectValue(getObjectTypeOrFail(o.getTypeID()), o.getValue())));
  }

  public ObjectEntity getObject(String type, String value) {
    ObjectTypeEntity objectType = getObjectTypeOrFail(type);

    // Encode value using EntityHandler because the mapping value is also stored encoded.
    ObjectByTypeValueEntity objectByTypeValue = objectAccessor.getObjectByTypeValue(objectType.getId(), encodeObjectValue(objectType, value));
    return ObjectUtils.ifNotNull(objectByTypeValue, o -> getObject(o.getObjectID()));
  }

  public ObjectEntity saveObject(ObjectEntity object) throws ImmutableViolationException {
    if (object == null) return null;

    ObjectTypeEntity type = getObjectTypeOrFail(object.getTypeID());

    // It's not allowed to create the same object multiple times.
    if (getObject(type.getName(), object.getValue()) != null) {
      throw new ImmutableViolationException("Object already exists.");
    }

    // Encode value using EntityHandler to store value in encoded format.
    // Clone entity first in order to not change supplied object instance.
    ObjectEntity persistent = object.clone()
            .setValue(encodeObjectValue(type, object.getValue()));

    // Also save an ObjectByTypeValue mapping.
    ObjectByTypeValueEntity objectByTypeValue = new ObjectByTypeValueEntity()
            .setObjectTypeID(persistent.getTypeID())
            .setObjectValue(persistent.getValue())
            .setObjectID(persistent.getId());

    objectMapper.save(persistent);
    objectByTypeValueMapper.save(objectByTypeValue);

    return object;
  }

  /* ObjectFactBindingEntity-related methods */

  public List<ObjectFactBindingEntity> fetchObjectFactBindings(UUID id) {
    return objectAccessor.fetchObjectFactBindings(id).all();
  }

  public ObjectFactBindingEntity saveObjectFactBinding(ObjectFactBindingEntity binding) throws ImmutableViolationException {
    if (binding == null) return null;
    if (getObject(binding.getObjectID()) == null)
      throw new IllegalArgumentException(String.format("Object with id = %s does not exist.", binding.getObjectID()));
    if (objectFactBindingMapper.get(binding.getObjectID(), binding.getFactID()) != null)
      throw new ImmutableViolationException("It is not allowed to update an ObjectFactBinding.");

    objectFactBindingMapper.save(binding);

    return binding;
  }

  /* Private helper methods */

  private LoadingCache<UUID, ObjectTypeEntity> createObjectTypeByIdCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<UUID, ObjectTypeEntity>() {
              @Override
              public ObjectTypeEntity load(UUID key) throws Exception {
                return ObjectUtils.notNull(objectTypeMapper.get(key), new Exception(String.format("ObjectType with id = %s does not exist.", key)));
              }
            });
  }

  private LoadingCache<String, ObjectTypeEntity> createObjectTypeByNameCache() {
    return CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, ObjectTypeEntity>() {
              @Override
              public ObjectTypeEntity load(String key) throws Exception {
                return ObjectUtils.notNull(objectTypeAccessor.getByName(key), new Exception(String.format("ObjectType with name = %s does not exist.", key)));
              }
            });
  }

  private ObjectTypeEntity getObjectTypeOrFail(UUID id) {
    try {
      return objectTypeByIdCache.get(id);
    } catch (ExecutionException e) {
      throw new IllegalArgumentException(e.getCause());
    }
  }

  private ObjectTypeEntity getObjectTypeOrFail(String name) {
    try {
      return objectTypeByNameCache.get(name);
    } catch (ExecutionException e) {
      throw new IllegalArgumentException(e.getCause());
    }
  }

  private String encodeObjectValue(ObjectTypeEntity type, String value) {
    return entityHandlerFactory.get(type.getEntityHandler(), type.getEntityHandlerParameter()).encode(value);
  }

  private String decodeObjectValue(ObjectTypeEntity type, String value) {
    return entityHandlerFactory.get(type.getEntityHandler(), type.getEntityHandlerParameter()).decode(value);
  }

}
