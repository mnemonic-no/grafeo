package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ObjectTypeByIdConverter implements Converter<UUID, ObjectType> {

  private final ObjectManager objectManager;
  private final Function<ObjectTypeEntity, ObjectType> objectTypeConverter;

  @Inject
  public ObjectTypeByIdConverter(ObjectManager objectManager, Function<ObjectTypeEntity, ObjectType> objectTypeConverter) {
    this.objectManager = objectManager;
    this.objectTypeConverter = objectTypeConverter;
  }

  @Override
  public Class<UUID> getSourceType() {
    return UUID.class;
  }

  @Override
  public Class<ObjectType> getTargetType() {
    return ObjectType.class;
  }

  @Override
  public ObjectType apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNotNull(objectManager.getObjectType(id), objectTypeConverter, ObjectType.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
