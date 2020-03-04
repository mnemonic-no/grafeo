package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;

import javax.inject.Inject;
import java.util.function.Function;

public class ObjectTypeConverter implements Function<ObjectTypeEntity, ObjectType> {

  private final NamespaceByIdConverter namespaceConverter;

  @Inject
  public ObjectTypeConverter(NamespaceByIdConverter namespaceConverter) {
    this.namespaceConverter = namespaceConverter;
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
