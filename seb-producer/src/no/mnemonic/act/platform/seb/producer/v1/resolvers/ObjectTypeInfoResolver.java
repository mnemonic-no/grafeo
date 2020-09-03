package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.seb.model.v1.ObjectTypeInfoSEB;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ObjectTypeInfoResolver implements Function<UUID, ObjectTypeInfoSEB> {

  private final ObjectManager objectManager;

  @Inject
  public ObjectTypeInfoResolver(ObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  @Override
  public ObjectTypeInfoSEB apply(UUID id) {
    if (id == null) return null;

    ObjectTypeEntity type = objectManager.getObjectType(id);
    if (type == null) return null;

    return ObjectTypeInfoSEB.builder()
            .setId(type.getId())
            .setName(type.getName())
            .build();
  }
}
