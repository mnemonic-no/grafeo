package no.mnemonic.services.grafeo.service.implementation.converters.response;

import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.NamespaceByIdResponseResolver;

import javax.inject.Inject;
import java.util.function.Function;

public class ObjectTypeResponseConverter implements Function<ObjectTypeEntity, ObjectType> {

  private final NamespaceByIdResponseResolver namespaceConverter;

  @Inject
  public ObjectTypeResponseConverter(NamespaceByIdResponseResolver namespaceConverter) {
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
            .setIndexOption(isTimeGlobalIndex(entity) ? ObjectType.IndexOption.TimeGlobal : ObjectType.IndexOption.Daily)
            .build();
  }

  private boolean isTimeGlobalIndex(ObjectTypeEntity entity) {
    return entity.isSet(ObjectTypeEntity.Flag.TimeGlobalIndex);
  }
}
