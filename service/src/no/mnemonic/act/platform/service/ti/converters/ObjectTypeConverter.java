package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ObjectTypeConverter implements Converter<ObjectTypeEntity, ObjectType> {

  private final Function<UUID, Namespace> namespaceConverter;

  @Inject
  public ObjectTypeConverter(Function<UUID, Namespace> namespaceConverter) {
    this.namespaceConverter = namespaceConverter;
  }

  @Override
  public Class<ObjectTypeEntity> getSourceType() {
    return ObjectTypeEntity.class;
  }

  @Override
  public Class<ObjectType> getTargetType() {
    return ObjectType.class;
  }

  @Override
  public ObjectType apply(ObjectTypeEntity entity) {
    if (entity == null) return null;
    return ObjectType.builder()
            .setId(entity.getId())
            .setNamespace(namespaceConverter.apply(entity.getNamespaceID()))
            .setName(entity.getName())
            .setValidator(entity.getValidator())
            .setValidatorParameter(entity.getValidatorParameter())
            .build();
  }
}
