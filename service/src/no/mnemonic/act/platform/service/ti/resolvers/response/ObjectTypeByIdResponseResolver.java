package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ObjectTypeByIdResponseResolver implements Function<UUID, ObjectType> {

  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;

  @Inject
  public ObjectTypeByIdResponseResolver(ObjectManager objectManager, ObjectTypeResponseConverter objectTypeResponseConverter) {
    this.objectManager = objectManager;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
  }

  @Override
  public ObjectType apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNotNull(objectManager.getObjectType(id), objectTypeResponseConverter, ObjectType.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
