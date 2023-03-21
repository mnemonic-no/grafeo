package no.mnemonic.services.grafeo.service.ti.resolvers.response;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.service.ti.converters.response.ObjectTypeResponseConverter;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ObjectTypeByIdResponseResolver implements Function<UUID, ObjectType> {

  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;
  private final Map<UUID, ObjectType> responseCache;

  @Inject
  public ObjectTypeByIdResponseResolver(ObjectManager objectManager,
                                        ObjectTypeResponseConverter objectTypeResponseConverter,
                                        Map<UUID, ObjectType> responseCache) {
    this.objectManager = objectManager;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
    this.responseCache = responseCache;
  }

  @Override
  public ObjectType apply(UUID id) {
    if (id == null) return null;
    return responseCache.computeIfAbsent(id, this::resolveUncached);
  }

  private ObjectType resolveUncached(UUID id) {
    return ObjectUtils.ifNotNull(objectManager.getObjectType(id), objectTypeResponseConverter, ObjectType.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
