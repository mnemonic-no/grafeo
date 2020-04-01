package no.mnemonic.act.platform.service.ti.resolvers.request;

import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;

public class ObjectTypeRequestResolver {

  private final ObjectManager objectManager;

  @Inject
  public ObjectTypeRequestResolver(ObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  /**
   * Fetch an existing ObjectType by ID.
   *
   * @param id UUID of ObjectType
   * @return Existing ObjectType
   * @throws ObjectNotFoundException Thrown if ObjectType cannot be found
   */
  public ObjectTypeEntity fetchExistingObjectType(UUID id) throws ObjectNotFoundException {
    ObjectTypeEntity entity = objectManager.getObjectType(id);
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("ObjectType with id = %s does not exist.", id),
        "object.type.not.exist", "id", ObjectUtils.ifNotNull(id, Object::toString, "NULL"));
    }
    return entity;
  }
}
