package no.mnemonic.services.grafeo.dao.facade.resolvers;

import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.bindings.DaoCache;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.facade.converters.ObjectRecordConverter;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * {@link CachedObjectResolver} implementation which is backed by a {@link Map}.
 */
public class MapBackedObjectResolver implements CachedObjectResolver {

  private final ObjectManager objectManager;
  private final ObjectRecordConverter objectRecordConverter;
  private final Map<UUID, ObjectRecord> objectByIdCache;
  private final Map<String, ObjectRecord> objectByTypeValueCache;

  @Inject
  public MapBackedObjectResolver(
          ObjectManager objectManager,
          ObjectRecordConverter objectRecordConverter,
          @DaoCache Map<UUID, ObjectRecord> objectByIdCache,
          @DaoCache Map<String, ObjectRecord> objectByTypeValueCache) {
    this.objectManager = objectManager;
    this.objectRecordConverter = objectRecordConverter;
    this.objectByIdCache = objectByIdCache;
    this.objectByTypeValueCache = objectByTypeValueCache;
  }

  @Override
  public ObjectRecord getObject(UUID id) {
    if (id == null) return null;

    return objectByIdCache.computeIfAbsent(id,
            key -> objectRecordConverter.fromEntity(objectManager.getObject(id)));
  }

  @Override
  public ObjectRecord getObject(String type, String value) {
    if (StringUtils.isBlank(type) || StringUtils.isBlank(value)) return null;

    return objectByTypeValueCache.computeIfAbsent(createCacheKey(type, value),
            key -> objectRecordConverter.fromEntity(objectManager.getObject(type, value)));
  }

  private String createCacheKey(String type, String value) {
    return type + "/" + value;
  }
}
