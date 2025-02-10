package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.seb.model.v1.ObjectTypeInfoSEB;

import jakarta.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ObjectTypeInfoDaoResolver implements Function<UUID, ObjectTypeInfoSEB> {

  private final ObjectManager objectManager;

  @Inject
  public ObjectTypeInfoDaoResolver(ObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  @Override
  public ObjectTypeInfoSEB apply(UUID id) {
    if (id == null) return null;

    ObjectTypeEntity type = objectManager.getObjectType(id);
    if (type == null) {
      return ObjectTypeInfoSEB.builder()
              .setId(id)
              .setName("N/A")
              .build();
    }

    return ObjectTypeInfoSEB.builder()
            .setId(type.getId())
            .setName(type.getName())
            .build();
  }
}
