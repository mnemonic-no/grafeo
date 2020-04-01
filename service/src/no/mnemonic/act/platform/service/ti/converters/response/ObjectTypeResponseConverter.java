package no.mnemonic.act.platform.service.ti.converters.response;

import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.resolvers.response.NamespaceByIdResponseResolver;

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
            .build();
  }
}
