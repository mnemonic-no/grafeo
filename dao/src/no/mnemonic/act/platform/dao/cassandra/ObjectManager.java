package no.mnemonic.act.platform.dao.cassandra;

import com.datastax.driver.mapping.Mapper;
import no.mnemonic.act.platform.dao.cassandra.accessors.ObjectAccessor;
import no.mnemonic.act.platform.dao.cassandra.accessors.ObjectTypeAccessor;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.entity.cassandra.ObjectByTypeValueEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

@Singleton
public class ObjectManager {

  private final Mapper<ObjectTypeEntity> objectTypeMapper;
  private final Mapper<ObjectEntity> objectMapper;
  private final Mapper<ObjectByTypeValueEntity> objectByTypeValueMapper;
  private final Mapper<ObjectFactBindingEntity> objectFactBindingMapper;
  private final ObjectTypeAccessor objectTypeAccessor;
  private final ObjectAccessor objectAccessor;

  // Maps the name of an ObjectType to it's UUID.
  private final AtomicReference<Map<String, UUID>> objectTypeCache = new AtomicReference<>();

  @Inject
  public ObjectManager(Provider<ClusterManager> provider) {
    ClusterManager clusterManager = provider.get();
    objectTypeMapper = clusterManager.getMapper(ObjectTypeEntity.class);
    objectMapper = clusterManager.getMapper(ObjectEntity.class);
    objectByTypeValueMapper = clusterManager.getMapper(ObjectByTypeValueEntity.class);
    objectFactBindingMapper = clusterManager.getMapper(ObjectFactBindingEntity.class);
    objectTypeAccessor = clusterManager.getAccessor(ObjectTypeAccessor.class);
    objectAccessor = clusterManager.getAccessor(ObjectAccessor.class);
  }

  /* ObjectTypeEntity-related methods */

  public ObjectTypeEntity getObjectType(UUID id) {
    return objectTypeMapper.get(id);
  }

  public List<ObjectTypeEntity> fetchObjectTypes() {
    return objectTypeAccessor.fetch().all();
  }

  public void saveObjectType(ObjectTypeEntity objectType) {
    if (objectType != null) {
      objectTypeMapper.save(objectType);
      objectTypeCache.set(null); // Invalidate cache. We just re-fetch everything when needed.
    }
  }

  /* ObjectEntity-related methods */

  //TODO: Apply entity handler.
  public ObjectEntity getObject(UUID id) {
    return objectMapper.get(id);
  }

  public ObjectEntity getObject(String type, String value) {
    UUID typeID = lookupObjectTypeID(type);
    if (typeID == null) {
      throw new IllegalArgumentException(String.format("ObjectType with type = %s does not exist.", type));
    }

    ObjectByTypeValueEntity objectByTypeValue = objectAccessor.getObjectByTypeValue(typeID, value);
    return ObjectUtils.ifNotNull(objectByTypeValue, o -> getObject(o.getObjectID()));
  }

  //TODO: Apply entity handler.
  public void saveObject(ObjectEntity object) throws ImmutableViolationException {
    if (object != null) {
      if (getObject(object.getId()) != null) {
        throw new ImmutableViolationException("Object already exists.");
      }

      ObjectByTypeValueEntity objectByTypeValue = new ObjectByTypeValueEntity()
              .setObjectTypeID(object.getTypeID())
              .setObjectValue(object.getValue())
              .setObjectID(object.getId());

      objectMapper.save(object);
      objectByTypeValueMapper.save(objectByTypeValue);
    }
  }

  /* ObjectFactBindingEntity-related methods */

  public List<ObjectFactBindingEntity> fetchObjectFactBindings(UUID id) {
    return objectAccessor.fetchObjectFactBindings(id).all();
  }

  public void saveObjectFactBinding(ObjectFactBindingEntity binding) {
    if (binding != null) {
      objectFactBindingMapper.save(binding);
    }
  }

  /* Private helper methods */

  private UUID lookupObjectTypeID(String type) {
    return objectTypeCache.updateAndGet(c -> ObjectUtils.ifNull(c, this::createObjectTypeCache)).get(type);
  }

  private Map<String, UUID> createObjectTypeCache() {
    return Collections.unmodifiableMap(MapUtils.map(fetchObjectTypes(), e -> T(e.getName(), e.getId())));
  }

}
